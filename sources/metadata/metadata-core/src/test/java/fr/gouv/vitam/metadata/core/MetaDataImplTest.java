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

import static fr.gouv.vitam.common.json.JsonHandler.toArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import fr.gouv.vitam.common.model.RequestResponse;
import org.bson.BsonDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.MetaData;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.DbRequest;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Result;
import fr.gouv.vitam.metadata.core.database.collections.ResultDefault;
import fr.gouv.vitam.metadata.core.database.collections.ResultError;
import fr.gouv.vitam.metadata.core.database.collections.Unit;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({DbRequestFactoryImpl.class, MongoDbAccessMetadataFactory.class})
public class MetaDataImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private MetaData metaDataImpl;
    private DbRequest request;
    private DbRequestFactory dbRequestFactory;
    private MongoDbAccessMetadataFactory mongoDbAccessFactory;

    private static final String DATA_INSERT = "{ \"data\": \"test\" }";
    private static final Integer TENANT_ID = 0;

    private static final String SAMPLE_OBJECTGROUP_FILENAME = "sample_objectGroup_document.json";
    private static final String SAMPLE_OBJECTGROUP_FILTERED_FILENAME = "sample_objectGroup_document_filtered.json";
    private static JsonNode sampleObjectGroup;
    private static JsonNode sampleObjectGroupFiltered;

    private static final String QUERY =
        "{ \"$queries\": [{ \"$path\": \"aaaaa\" }],\"$filter\": { },\"$projection\": {}}";
    private static final String REQUEST_TEST =
        "{ \"$roots\" : [ \"id0\" ], \"$query\" : [ " + "{ \"$path\" : [ \"id1\", \"id2\"] }," +
            "{ \"$and\" : [ " + "{\"$exists\" : \"mavar1\"}, " + "{\"$missing\" : \"mavar2\"}, " +
            "{\"$isNull\" : \"mavar3\"}, " +
            "{ \"$or\" : [ " +
            "{\"$in\" : { \"mavar4\" : [1, 2, \"maval1\"] }}, " +
            "{ \"$nin\" : { \"mavar5\" : [\"maval2\", true] } } ] } ] }," +
            "{ \"$not\" : [ " + "{ \"$size\" : { \"mavar5\" : 5 } }, " + "{ \"$gt\" : { \"mavar6\" : 7 } }, " +
            "{ \"$lte\" : { \"mavar7\" : 8 } } ] , \"$exactdepth\" : 4}," + "{ \"$not\" : [ " +
            "{ \"$eq\" : { \"mavar8\" : 5 } }, " +
            "{ \"$ne\" : { \"mavar9\" : \"ab\" } }, " +
            "{ \"$range\" : { \"mavar10\" : { \"$gte\" : 12, \"$lte\" : 20} } } ], \"$depth\" : 1}, " +
            "{ \"$and\" : [ { \"$term\" : { \"mavar14\" : \"motMajuscule\", \"mavar15\" : \"simplemot\" } } ] }, " +
            "{ \"$regex\" : { \"mavar14\" : \"^start?aa.*\" } } " + "], " +
            "\"$filter\" : {\"$offset\" : 100, \"$limit\" : 1000, \"$hint\" : [\"cache\"], " +
            "\"$orderby\" : { \"maclef1\" : 1 , \"maclef2\" : -1,  \"maclef3\" : 1 } }," +
            "\"$projection\" : {\"$fields\" : {\"#dua\" : 1, \"#all\" : 1}, \"$usage\" : \"abcdef1234\" } }";



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
        PowerMockito.mockStatic(MongoDbAccessMetadataFactory.class);
        mongoDbAccessFactory = mock(MongoDbAccessMetadataFactory.class);
        PowerMockito.mockStatic(DbRequestFactoryImpl.class);
        dbRequestFactory = mock(DbRequestFactoryImpl.class);
        PowerMockito.when(DbRequestFactoryImpl.getInstance()).thenReturn(dbRequestFactory);
        when(dbRequestFactory.create()).thenReturn(request);
        when(MongoDbAccessMetadataFactory.create(null)).thenReturn(null);

    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertUnitWhenDuplicateEntryThenThrowMetaDataAlreadyExistException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertObjectGroupWhenDuplicateEntryThenThrowMetaDataAlreadyExistException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }


    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertUnitWhenInstantiationExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertObjectGroupWhenInstantiationExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenInsertUnitWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        final MongoWriteException error =
            new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        when(request.execRequest(anyObject(), anyObject())).thenThrow(error);

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenInsertObjectGroupWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        final MongoWriteException error =
            new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        when(request.execRequest(anyObject(), anyObject())).thenThrow(error);

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertUnitWhenIllegalAccessExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertObjectGroupWhenIllegalAccessExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertUnitWhenStringTooLongThenThrowMetaDataDocumentSizeException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
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
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
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
        when(request.execRequest(anyObject(), anyObject())).thenReturn(new ResultError(FILTERARGS.UNITS));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_Select_Unit_When_InstantiationException_ThenThrown_MetaDataExecutionException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(REQUEST_TEST));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_SelectUnitWhenStringTooLong_Then_Throw_InvalidParseOperationException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
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
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(QUERY));
    }


    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnits_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(QUERY));
    }

    @RunWithCustomExecutor
    @Test
    public void given_filled_query_When_SelectUnitById_() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.selectUnitsById(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test
    public void given_filled_query_When_UpdateUnitById_() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataExecutionException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(""));
    }



    @Test(expected = MetaDataNotFoundException.class)
    public void givenInsertObjectGroupWhenParentNotFoundThenThrowMetaDataNotFoundException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenReturn(new ResultError(FILTERARGS.UNITS));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_selectUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.selectUnitsById(JsonHandler.getFromString(""), "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_updateUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(""), "unitId");
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataExecutionException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_FindMetadataNotFoundException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataNotFoundException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnit_FindMetadataNotFoundException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataNotFoundException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.selectUnitsById(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(QUERY), "unitId");
    }

    public void given_empty_query_UpdateUnitbyId_When_IllegalAccessException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(""), "");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_UpdateUnitWhenStringTooLong_Then_Throw_InvalidParseOperationException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        final int oldValue = GlobalDatasParser.limitRequest;
        try {
            GlobalDatasParser.limitRequest = 1000;
            metaDataImpl.updateUnitbyId(JsonHandler.getFromString(createLongString(1001)), "unitId");
        } finally {
            GlobalDatasParser.limitRequest = oldValue;
        }
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_Update_Unit_When_InstantiationException_ThenThrown_MetaDataExecutionException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(REQUEST_TEST), "");
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void given_updateUnitbyId_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test
    public void testSelectObjectGroupById() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final Result result = new ResultDefault(FILTERARGS.OBJECTGROUPS);
        result.addId("ogId", (float) 1);
        result.addFinal(new ObjectGroup(sampleObjectGroup));
        when(request.execRequest(anyObject(), anyObject())).thenReturn(result);
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        RequestResponse<JsonNode> requestResponse = metaDataImpl.selectObjectGroupById(JsonHandler.getFromString(QUERY), "ogId");
        assertTrue(requestResponse.isOk());
        RequestResponseOK<JsonNode> jsonNode = (RequestResponseOK<JsonNode>)requestResponse;
        final JsonNode objectGroupDocument = jsonNode.getResults().iterator().next();
        boolean different = false;
        Iterator<Entry<String, JsonNode>> fields = objectGroupDocument.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            if (!sampleObjectGroupFiltered.has(entry.getKey()) || !sampleObjectGroupFiltered.get(entry.getKey()).asText().equals(entry.getValue().asText())) {
                System.err.println("D: " + entry.getKey() + " : " + entry.getValue() + " " + entry.getValue().getNodeType());
                different = true;
            }
        }
        fields = sampleObjectGroupFiltered.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            if (!objectGroupDocument.has(entry.getKey()) || !objectGroupDocument.get(entry.getKey()).asText().equals(entry.getValue().asText())) {
                System.err.println("S: " + entry.getKey() + " : " + entry.getValue() + " " + entry.getValue().getNodeType());
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

        when(request.execRequest(Matchers.isA(UpdateParserMultiple.class), anyObject())).thenReturn(updateResult);
        when(request.execRequest(Matchers.isA(SelectParserMultiple.class), anyObject())).thenReturn(firstSelectResult,
            secondSelectResult);
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory);
        RequestResponse<JsonNode> requestResponse = metaDataImpl.updateUnitbyId(updateRequest, "unitId");
        assertTrue(requestResponse.isOk());
        List<JsonNode> ret = ((RequestResponseOK<JsonNode>)requestResponse).getResults();

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


}
