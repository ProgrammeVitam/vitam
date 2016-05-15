/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.BsonDocument;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;

import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.core.database.collections.DbRequest;
import fr.gouv.vitam.core.database.collections.ResultError;
import fr.gouv.vitam.parser.request.parser.GlobalDatasParser;

public class MetaDataImplTest {

    private MetaDataImpl metaDataImpl;
    private DbRequest request;
    private DbRequestFactory dbRequestFactory;
    private MongoDbAccessFactory mongoDbAccessFactory;
    
    // TODO REVIEW UPPERCASE
    private static final String dataInsert = "{ \"data\": \"test\" }";

    private static final String buildQueryWithOptions(String query, String data) {
        return new StringBuilder()
                .append("{ $roots : [ '' ], ")
                .append("$query : [ " + query + " ], ")
                .append("$data : " + data + " }")
                .toString();
    }

    private static String createLongString(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Before
    public void setUp() {
        request = mock(DbRequest.class);
        mongoDbAccessFactory = mock(MongoDbAccessFactory.class);
        dbRequestFactory = mock(DbRequestFactory.class);
        when(dbRequestFactory.create()).thenReturn(request);
        when(mongoDbAccessFactory.create(null)).thenReturn(null);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertUnitWhenDuplicateEntryThenThrowMetaDataAlreadyExistException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InvalidParseOperationException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryWithOptions("", dataInsert));
    }
    
    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertUnitWhenInstantiationExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new InstantiationException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryWithOptions("", dataInsert));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenInsertUnitWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        MongoWriteException error = new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        when(request.execRequest(anyObject(), anyObject())).thenThrow(error);

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryWithOptions("", dataInsert));
    }
    
    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertUnitWhenIllegalAccessExceptionThenThrowMetaDataExecutionException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenThrow(new IllegalAccessException());

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryWithOptions("", dataInsert));
    }
    
    @Test(expected = MetaDataDocumentSizeException.class)
    public void givenInsertUnitWhenStringTooLongThenThrowMetaDataDocumentSizeException() throws Exception {
        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        GlobalDatasParser.limitRequest = 1000;
        
        metaDataImpl.insertUnit(createLongString(1001));
        // FIXME REVIEW should reset limitRequest to previous default value
    }
    
    @Test(expected = MetaDataNotFoundException.class)
    public void givenInsertUnitWhenParentNotFoundThenThrowMetaDataNotFoundException() throws Exception {
        when(request.execRequest(anyObject(), anyObject())).thenReturn(new ResultError(FILTERARGS.UNITS));

        metaDataImpl = new MetaDataImpl(null, mongoDbAccessFactory, dbRequestFactory);
        metaDataImpl.insertUnit(buildQueryWithOptions("", dataInsert));
    }
}
