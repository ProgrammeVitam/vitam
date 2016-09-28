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
package fr.gouv.vitam.core;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.BsonDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;

import fr.gouv.vitam.api.MetaData;
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.core.database.collections.DbRequest;
import fr.gouv.vitam.core.database.collections.ObjectGroup;
import fr.gouv.vitam.core.database.collections.Result;
import fr.gouv.vitam.core.database.collections.ResultDefault;
import fr.gouv.vitam.core.database.collections.ResultError;
import fr.gouv.vitam.core.database.collections.Unit;

public class MetaDataImplTest {

    private MetaData metaDataImpl;
    private DbRequest request;
    private DbRequestFactory dbRequestFactory;
    private MongoDbAccessMetadataFactory mongoDbAccessFactory;

    private static final String DATA_INSERT = "{ \"data\": \"test\" }";

    private static final String SAMPLE_OBJECTGROUP_FILENAME = "sample_objectGroup_document.json";
    private static JsonNode sampleObjectGroup;

    private static final String QUERY =
        "{ \"$queries\": [{ \"$path\": \"aaaaa\" }],\"$filter\": { },\"$projection\": {}}";
    private static final String REQUEST_TEST =
        "{ $roots : [ 'id0' ], $query : [ " + "{ $path : [ 'id1', 'id2'] }," +
            "{ $and : [ " + "{$exists : 'mavar1'}, " + "{$missing : 'mavar2'}, " + "{$isNull : 'mavar3'}, " +
            "{ $or : [ " +
            "{$in : { 'mavar4' : [1, 2, 'maval1'] }}, " + "{ $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
            "{ $not : [ " + "{ $size : { 'mavar5' : 5 } }, " + "{ $gt : { 'mavar6' : 7 } }, " +
            "{ $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," + "{ $not : [ " + "{ $eq : { 'mavar8' : 5 } }, " +
            "{ $ne : { 'mavar9' : 'ab' } }, " +
            "{ $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' } } " + "], " +
            "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
            "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
            "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' } }";



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
    }

    @Before
    public void setUp() throws Exception {
        request = mock(DbRequest.class);
        mongoDbAccessFactory = mock(MongoDbAccessMetadataFactory.class);
        dbRequestFactory = mock(DbRequestFactory.class);
        when(dbRequestFactory.create()).thenReturn(request);
        when(mongoDbAccessFactory.create(null)).thenReturn(null);

    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertUnitWhenDuplicateEntryThenThrowMetaDataAlreadyExistException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertObjectGroupWhenDuplicateEntryThenThrowMetaDataAlreadyExistException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }


    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertUnitWhenInstantiationExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertObjectGroupWhenInstantiationExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenInsertUnitWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        final MongoWriteException error =
            new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        when(request.execRequest(anyObject(), anyObject())).thenThrow(error);

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenInsertObjectGroupWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        final MongoWriteException error =
            new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        when(request.execRequest(anyObject(), anyObject())).thenThrow(error);

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertUnitWhenIllegalAccessExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertObjectGroupWhenIllegalAccessExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void givenInsertUnitWhenStringTooLongThenThrowMetaDataDocumentSizeException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        String bigData = "{ \"data\": \"" + createLongString(1001) + "\" }";
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", bigData));
        GlobalDatasParser.limitRequest = GlobalDatasParser.DEFAULT_LIMIT_REQUEST;
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void givenInsertObjectGroupWhenStringTooLongThenThrowMetaDataDocumentSizeException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        String bigData = "{ \"data\": \"" + createLongString(1001) + "\" }";
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", bigData));
        GlobalDatasParser.limitRequest = GlobalDatasParser.DEFAULT_LIMIT_REQUEST;
    }

    @Test(expected = MetaDataNotFoundException.class)
    public void givenInsertUnitWhenParentNotFoundThenThrowMetaDataNotFoundException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenReturn(new ResultError(FILTERARGS.UNITS));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_Select_Unit_When_InstantiationException_ThenThrown_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery(REQUEST_TEST);
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_SelectUnitWhenStringTooLong_Then_Throw_MetaDataDocumentSizeException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        metaDataImpl.selectUnitsByQuery(createLongString(1001));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_selectUnitquery_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery(QUERY);
    }


    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnits_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery(QUERY);
    }

    @Test
    public void given_filled_query_When_SelectUnitById_() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsById(QUERY, "unitId");
    }

    @Test
    public void given_filled_query_When_UpdateUnitById_() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataExecutionException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery(QUERY);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery("");
    }



    @Test(expected = MetaDataNotFoundException.class)
    public void givenInsertObjectGroupWhenParentNotFoundThenThrowMetaDataNotFoundException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenReturn(new ResultError(FILTERARGS.UNITS));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_selectUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsById("", "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_updateUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId("", "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataExecutionException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_FindMetadataNotFoundException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataNotFoundException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnit_FindMetadataNotFoundException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataNotFoundException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsById(QUERY, "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    public void given_empty_query_UpdateUnitbyId_When_IllegalAccessException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId("", "");
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_UpdateUnitWhenStringTooLong_Then_Throw_MetaDataDocumentSizeException() throws Exception {
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        metaDataImpl.updateUnitbyId(createLongString(1001), "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_Update_Unit_When_InstantiationException_ThenThrown_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(REQUEST_TEST, "");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_updateUnitbyId_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    @Test
    public void testSelectObjectGroupById() throws Exception {
        Result result = new ResultDefault(FILTERARGS.OBJECTGROUPS);
        result.addId("ogId");
        result.setNbResult(1);
        result.addFinal(new ObjectGroup(sampleObjectGroup));
        when(request.execRequest(anyObject(), anyObject())).thenReturn(result);
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        JsonNode jsonNode = metaDataImpl.selectObjectGroupById(QUERY, "ogId");
        ArrayNode resultArray = (ArrayNode) jsonNode.get("$result");
        assertEquals(1,resultArray.size());
        ObjectNode objectGroupDocument = (ObjectNode) resultArray.get(0);
        String resultedObjectGroup = JsonHandler.unprettyPrint(objectGroupDocument);
        String expectedObjectGroup = JsonHandler.unprettyPrint(sampleObjectGroup);
        assertEquals(expectedObjectGroup, resultedObjectGroup);
    }

    @Test
    public void testDiffResultOnUpdate() throws Exception {
        String wanted = "{\"$hint\":{\"total\":1,\"size\":1,\"limit\":1,\"time_out\":false},\"$context\":{}," +
            "\"$result\":[{\"_id\":\"unitId\",\"_diff\":\"-    \\\"title\\\" : \\\"title\\\",\\n-    " +
            "\\\"description\\\" : \\\"description\\\"\\n+    \\\"title\\\" : \\\"MODIFIED title\\\",\\n+    " +
            "\\\"description\\\" : \\\"MODIFIED description\\\"\"}]}";

        Result updateResult = new ResultDefault(FILTERARGS.UNITS);
        updateResult.addId("unitId");
        updateResult.setNbResult(1);

        Result firstSelectResult = new ResultDefault(FILTERARGS.UNITS);
        firstSelectResult.addId("unitId");
        firstSelectResult.setNbResult(1);
        Unit unit = new Unit();
        unit.put("_id", "unitId");
        unit.put("title", "title");
        unit.put("description", "description");
        firstSelectResult.addFinal(unit);

        Result secondSelectResult = new ResultDefault(FILTERARGS.UNITS);
        secondSelectResult.addId("unitId");
        secondSelectResult.setNbResult(1);
        Unit secondUnit = new Unit();
        secondUnit.put("_id", "unitId");
        secondUnit.put("title", "MODIFIED title");
        secondUnit.put("description", "MODIFIED description");
        secondSelectResult.addFinal(secondUnit);

        when(request.execRequest(Matchers.isA(UpdateParserMultiple.class), anyObject())).thenReturn(updateResult);
        when(request.execRequest(Matchers.isA(SelectParserMultiple.class), anyObject())).thenReturn
            (firstSelectResult, secondSelectResult);
        metaDataImpl = MetaDataImpl.newMetadata(null, mongoDbAccessFactory, dbRequestFactory);
        JsonNode ret = metaDataImpl.updateUnitbyId("{\"$roots\":[\"#id\"],\"$query\":[],\"$filter\":{}," +
            "\"$action\":[{\"$set\":{\"title\":\"MODIFIED TITLE\", \"description\":\"MODIFIED DESCRIPTION\"}}]}","unitId");

        assertEquals(wanted, ret.toString());
    }
}
