/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.BsonDocument;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;

import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.core.database.collections.DbRequest;
import fr.gouv.vitam.core.database.collections.ResultError;

public class MetaDataImplTest {

    private MetaDataImpl metaDataImpl;
    private DbRequest request;
    private DbRequestFactory dbRequestFactory;
    private MongoDbAccessMetadataFactory mongoDbAccessFactory;

    // TODO REVIEW UPPERCASE
    private static final String dataInsert = "{ \"data\": \"test\" }";

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



    private static final JsonNode buildQueryJsonWithOptions(String query, String data)
        throws InvalidParseOperationException {
        return JsonHandler.getFromString(new StringBuilder()
            .append("{ $roots : [ '' ], ")
            .append("$query : [ " + query + " ], ")
            .append("$data : " + data + " }")
            .toString());
    }

    private static String createLongString(int size) {
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }

        return sb.toString();
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

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertObjectGroupWhenDuplicateEntryThenThrowMetaDataAlreadyExistException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", dataInsert));
    }


    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertUnitWhenInstantiationExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertObjectGroupWhenInstantiationExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenInsertUnitWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        final MongoWriteException error =
            new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        when(request.execRequest(anyObject(), anyObject())).thenThrow(error);

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenInsertObjectGroupWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        final MongoWriteException error =
            new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        when(request.execRequest(anyObject(), anyObject())).thenThrow(error);

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertUnitWhenIllegalAccessExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertObjectGroupWhenIllegalAccessExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void givenInsertUnitWhenStringTooLongThenThrowMetaDataDocumentSizeException() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        String bigData = "{ \"data\": \"" + createLongString(1001) + "\" }";
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", bigData));
        GlobalDatasParser.limitRequest = GlobalDatasParser.DEFAULT_LIMIT_REQUEST;
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void givenInsertObjectGroupWhenStringTooLongThenThrowMetaDataDocumentSizeException() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        String bigData = "{ \"data\": \"" + createLongString(1001) + "\" }";
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", bigData));
        GlobalDatasParser.limitRequest = GlobalDatasParser.DEFAULT_LIMIT_REQUEST;
    }

    @Test(expected = MetaDataNotFoundException.class)
    public void givenInsertUnitWhenParentNotFoundThenThrowMetaDataNotFoundException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenReturn(new ResultError(FILTERARGS.UNITS));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_Select_Unit_When_InstantiationException_ThenThrown_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery(REQUEST_TEST);
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_SelectUnitWhenStringTooLong_Then_Throw_MetaDataDocumentSizeException() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        metaDataImpl.selectUnitsByQuery(createLongString(1001));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_selectUnitquery_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery(QUERY);
    }


    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnits_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery(QUERY);
    }

    @Test
    public void given_filled_query_When_SelectUnitById_() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsById(QUERY, "unitId");
    }

    @Test
    public void given_filled_query_When_UpdateUnitById_() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataExecutionException(""));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery(QUERY);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsByQuery("");
    }



    @Test(expected = MetaDataNotFoundException.class)
    public void givenInsertObjectGroupWhenParentNotFoundThenThrowMetaDataNotFoundException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenReturn(new ResultError(FILTERARGS.UNITS));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", dataInsert));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_selectUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsById("", "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_updateUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId("", "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataExecutionException(""));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_FindMetadataNotFoundException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataNotFoundException(""));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnit_FindMetadataNotFoundException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new MetaDataNotFoundException(""));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.selectUnitsById(QUERY, "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

    public void given_empty_query_UpdateUnitbyId_When_IllegalAccessException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId("", "");
    }

    @Test(expected = MetaDataDocumentSizeException.class)
    public void given_UpdateUnitWhenStringTooLong_Then_Throw_MetaDataDocumentSizeException() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        metaDataImpl.updateUnitbyId(createLongString(1001), "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_Update_Unit_When_InstantiationException_ThenThrown_MetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(REQUEST_TEST, "");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_updateUnitbyId_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException(""));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.updateUnitbyId(QUERY, "unitId");
    }

}
