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
package fr.gouv.vitam.core.database.collections.translator.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.bson.conversions.Bson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.parser.request.parser.DeleteParser;
import fr.gouv.vitam.parser.request.parser.InsertParser;
import fr.gouv.vitam.parser.request.parser.SelectParser;
import fr.gouv.vitam.parser.request.parser.UpdateParser;

@SuppressWarnings("javadoc")
public class RequestToMongodbTest {
    private static final String exampleSelectMd = "{ $roots : [ 'id0' ], $query : [ " + "{ $path : [ 'id1', 'id2'] }," +
        "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
        "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " + "], " +
        "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
        "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
        "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' } }";

    private static final String exampleDeleteMd = "{ $roots : [ 'id0' ], $query : [ " + "{ $path : [ 'id1', 'id2'] }," +
        "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
        "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " + "], " + "$filter : {$mult : false } }";

    private static final String data =
        "{ " + "\"address\": { \"streetAddress\": \"21 2nd Street\", \"city\": \"New York\" }, " +
            "\"phoneNumber\": [ { \"location\": \"home\", \"code\": 44 } ] }";

    private static final String exampleInsertMd = "{ $roots : [ 'id0' ], $query : [ " + "{ $path : [ 'id1', 'id2'] }," +
        "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
        "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " + "], " + "$filter : {$mult : false }," +
        "$data : " + data + " }";

    private static final String updateAction =
        "[ " + "{ $set : { mavar1 : 1, mavar2 : 1.2, mavar3 : true, mavar4 : 'ma chaine' } }," +
            "{ $unset : [ 'mavar5', 'mavar6' ] }," + "{ $inc : { mavar7 : 2 } }," + "{ $min : { mavar8 : 3 } }," +
            "{ $min : { mavar16 : 12 } }," + "{ $max : { mavar9 : 3 } }," + "{ $rename : { mavar10 : 'mavar11' } }," +
            "{ $push : { mavar12 : { $each : [ 1, 2 ] } } }," + "{ $add : { mavar13 : { $each : [ 1, 2 ] } } }," +
            "{ $pop : { mavar14 : -1 } }," + "{ $pull : { mavar15 : { $each : [ 1, 2 ] } } } ]";

    private static final String exampleUpdateMd = "{ $roots : [ 'id0' ], $query : [ " + "{ $path : [ 'id1', 'id2'] }," +
        "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
        "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " + "], " + "$filter : {$mult : false }," +
        "$action : " + updateAction + " }";


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    private SelectToMongodb createSelect() {
        try {
            final SelectParser request1 = new SelectParser();
            request1.parse(exampleSelectMd);
            assertNotNull(request1);
            return new SelectToMongodb(request1);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    private InsertToMongodb createInsert() {
        try {
            final InsertParser request1 = new InsertParser();
            request1.parse(exampleInsertMd);
            assertNotNull(request1);
            return new InsertToMongodb(request1);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    private UpdateToMongodb createUpdate() {
        try {
            final UpdateParser request1 = new UpdateParser();
            request1.parse(exampleUpdateMd);
            assertNotNull(request1);
            return new UpdateToMongodb(request1);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    private DeleteToMongodb createDelete() {
        try {
            final DeleteParser request1 = new DeleteParser();
            request1.parse(exampleDeleteMd);
            assertNotNull(request1);
            return new DeleteToMongodb(request1);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void testGetRequestToMongoDb() {
        try {
            final SelectParser request1 = new SelectParser();
            request1.parse(exampleSelectMd);
            assertNotNull(request1);
            assertTrue(RequestToMongodb.getRequestToMongoDb(request1) instanceof SelectToMongodb);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final InsertParser request1 = new InsertParser();
            request1.parse(exampleInsertMd);
            assertNotNull(request1);
            assertTrue(RequestToMongodb.getRequestToMongoDb(request1) instanceof InsertToMongodb);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final UpdateParser request1 = new UpdateParser();
            request1.parse(exampleUpdateMd);
            assertNotNull(request1);
            assertTrue(RequestToMongodb.getRequestToMongoDb(request1) instanceof UpdateToMongodb);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final DeleteParser request1 = new DeleteParser();
            request1.parse(exampleDeleteMd);
            assertNotNull(request1);
            assertTrue(RequestToMongodb.getRequestToMongoDb(request1) instanceof DeleteToMongodb);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetCommands() {
        try {
            final SelectToMongodb rtm = createSelect();
            final Bson bsonRoot = rtm.getInitialRoots("_up");
            final int size = rtm.getNbQueries();
            for (int i = 0; i < size; i++) {
                final Bson bsonQuery = rtm.getNthQueries(i);
                final Bson pseudoRequest = rtm.getRequest(bsonRoot, bsonQuery);
                System.out.println(i + " = " + MongoDbHelper.bsonToString(pseudoRequest, false));
            }
            try {
                rtm.getNthQueries(size);
                fail("Should failed");
            } catch (final IllegalAccessError e) {

            }
            assertNotNull(rtm.getRequest());
            assertNotNull(rtm.getNthQuery(0));
            assertNotNull(rtm.getRequestParser());
            assertNotNull(rtm.model());
            System.out.println("OrderBy = " + MongoDbHelper.bsonToString(rtm.getFinalOrderBy(), false));
            System.out.println("Projection = " + MongoDbHelper.bsonToString(rtm.getFinalProjection(), false));
            System.out.println("Select Context = " + rtm.getLastDepth() + ":" + rtm.getFinalLimit() + ":" +
                rtm.getFinalOffset() + ":" + rtm.getUsage() + ":" + rtm.getHints());
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final InsertToMongodb rtm = createInsert();
            final Bson bsonRoot = rtm.getInitialRoots("_up");
            final int size = rtm.getNbQueries();
            for (int i = 0; i < size; i++) {
                final Bson bsonQuery = rtm.getNthQueries(i);
                final Bson pseudoRequest = rtm.getRequest(bsonRoot, bsonQuery);
                System.out.println(i + " = " + MongoDbHelper.bsonToString(pseudoRequest, false));
            }
            System.out.println("Data = " + MongoDbHelper.bsonToString(rtm.getFinalData(), false));
            System.out.println("Insert Context = " + rtm.getLastDepth() + ":" + rtm.isMultiple());
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final UpdateToMongodb rtm = createUpdate();
            final Bson bsonRoot = rtm.getInitialRoots("_up");
            final int size = rtm.getNbQueries();
            for (int i = 0; i < size; i++) {
                final Bson bsonQuery = rtm.getNthQueries(i);
                final Bson pseudoRequest = rtm.getRequest(bsonRoot, bsonQuery);
                System.out.println(i + " = " + MongoDbHelper.bsonToString(pseudoRequest, false));
            }
            System.out.println("Update Actons = " + MongoDbHelper.bsonToString(rtm.getFinalUpdate(), false));
            System.out.println("Update Context = " + rtm.getLastDepth() + ":" + rtm.isMultiple());
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final DeleteToMongodb rtm = createDelete();
            final Bson bsonRoot = rtm.getInitialRoots("_up");
            final int size = rtm.getNbQueries();
            for (int i = 0; i < size; i++) {
                final Bson bsonQuery = rtm.getNthQueries(i);
                final Bson pseudoRequest = rtm.getRequest(bsonRoot, bsonQuery);
                System.out.println(i + " = " + MongoDbHelper.bsonToString(pseudoRequest, false));
            }
            System.out.println("Delete Context = " + rtm.getLastDepth() + ":" + rtm.isMultiple());
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testSelectGetFinalOrderby() throws InvalidParseOperationException {
        final String example1 = "{ $roots : [], $query : [], $filter : {$orderby : {}}, $projection : {} }";
        final SelectParser request1 = new SelectParser();
        request1.parse(example1);
        final SelectToMongodb rtm1 = new SelectToMongodb(request1);
        assertEquals("", MongoDbHelper.bsonToString(rtm1.getFinalOrderBy(), false));

        final String example2 = "{ $roots : [], $query : [], $filter : {$orderby : {maclef : 1}}, $projection : {} }";
        final SelectParser request2 = new SelectParser();
        request2.parse(example2);
        final SelectToMongodb rtm2 = new SelectToMongodb(request2);
        assertEquals("{ \"maclef\" : 1 }", MongoDbHelper.bsonToString(rtm2.getFinalOrderBy(), false));

        final String example3 = "{ $roots : [], $query : [], $filter : {$orderby : {maclef : -1}}, $projection : {} }";
        final SelectParser request3 = new SelectParser();
        request3.parse(example3);
        final SelectToMongodb rtm3 = new SelectToMongodb(request3);
        assertEquals("{ \"maclef\" : -1 }", MongoDbHelper.bsonToString(rtm3.getFinalOrderBy(), false));
    }

    @Test
    public void testSelectGetFianlProjection() throws InvalidParseOperationException {
        final String example1 =
            "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : 1, #all : -1}} }";
        final SelectParser request1 = new SelectParser();
        request1.parse(example1);
        final SelectToMongodb rtm1 = new SelectToMongodb(request1);
        assertEquals("{ \"#dua\" : 1, \"#all\" : 0 }", MongoDbHelper.bsonToString(rtm1.getFinalProjection(), false));

        final String example2 = "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : 1}} }";
        final SelectParser request2 = new SelectParser();
        request2.parse(example2);
        final SelectToMongodb rtm2 = new SelectToMongodb(request2);
        assertEquals("{ \"#dua\" : 1 }", MongoDbHelper.bsonToString(rtm2.getFinalProjection(), false));

        final String example3 = "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : -1}} }";
        final SelectParser request3 = new SelectParser();
        request3.parse(example3);
        final SelectToMongodb rtm3 = new SelectToMongodb(request3);
        assertEquals("{ \"#dua\" : 0 }", MongoDbHelper.bsonToString(rtm3.getFinalProjection(), false));
    }

    @Test
    public void testRequestToAbstract() throws Exception {
        final InsertToMongodb rtm = createInsert();
        assertEquals(null, rtm.getHints());
        assertEquals(false, rtm.hasFullTextQuery());
        assertEquals(false, rtm.hintCache());
        assertEquals(false, rtm.hintNoTimeout());
    }

}
