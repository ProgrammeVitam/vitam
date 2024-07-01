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
package fr.gouv.vitam.common.database.translators.mongodb;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.parser.request.multiple.DeleteParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.bson.conversions.Bson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class RequestToMongodbTest {

    private static JsonNode exampleSelectMd;

    private static JsonNode exampleDeleteMd;

    private static JsonNode data;

    private static JsonNode exampleInsertMd;

    private static JsonNode updateAction;

    private static JsonNode exampleUpdateMd;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        exampleSelectMd = JsonHandler.getFromString(
            "{ $roots : [ 'id0' ], $query : [ " +
            "{ $path : [ 'id1', 'id2'] }," +
            "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
            "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
            "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " +
            "], " +
            "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
            "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
            "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' } }"
        );

        data = JsonHandler.getFromString(
            "{ " +
            "\"address\": { \"streetAddress\": \"21 2nd Street\", \"city\": \"New York\" }, " +
            "\"phoneNumber\": [ { \"location\": \"home\", \"code\": 44 } ] }"
        );

        exampleDeleteMd = JsonHandler.getFromString(
            "{ $roots : [ 'id0' ], $query : [ " +
            "{ $path : [ 'id1', 'id2'] }," +
            "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
            "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
            "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " +
            "], " +
            "$filter : {$mult : false } }"
        );

        exampleInsertMd = JsonHandler.getFromString(
            "{ $roots : [ 'id0' ], $query : [ " +
            "{ $path : [ 'id1', 'id2'] }," +
            "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
            "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
            "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " +
            "], " +
            "$filter : {$mult : false }," +
            "$data : " +
            data +
            " }"
        );

        updateAction = JsonHandler.getFromString(
            "[ " +
            "{ $set : { mavar1 : 1, mavar2 : 1.2, mavar3 : true, mavar4 : 'ma chaine' } }," +
            "{ $unset : [ 'mavar5', 'mavar6' ] }," +
            "{ $inc : { mavar7 : 2 } }," +
            "{ $min : { mavar8 : 3 } }," +
            "{ $min : { mavar16 : 12 } }," +
            "{ $max : { mavar9 : 3 } }," +
            "{ $rename : { mavar10 : 'mavar11' } }," +
            "{ $push : { mavar12 : [ 1, 2 ] } }," +
            "{ $add : { mavar13 : [ 1, 2 ] } }," +
            "{ $pop : { mavar14 : -1 } }," +
            "{ $pull : { mavar15 : [ 1, 2 ] } } ]"
        );

        exampleUpdateMd = JsonHandler.getFromString(
            "{ $roots : [ 'id0' ], $query : [ " +
            "{ $path : [ 'id1', 'id2'] }," +
            "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
            "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
            "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " +
            "], " +
            "$filter : {$mult : false }," +
            "$action : " +
            updateAction +
            " }"
        );
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    private SelectToMongodb createSelect() {
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
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
            final InsertParserMultiple request1 = new InsertParserMultiple();
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
            final UpdateParserMultiple request1 = new UpdateParserMultiple();
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
            final DeleteParserMultiple request1 = new DeleteParserMultiple();
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
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(exampleSelectMd);
            assertNotNull(request1);
            assertTrue(RequestToMongodb.getRequestToMongoDb(request1) instanceof SelectToMongodb);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final InsertParserMultiple request1 = new InsertParserMultiple();
            request1.parse(exampleInsertMd);
            assertNotNull(request1);
            assertTrue(RequestToMongodb.getRequestToMongoDb(request1) instanceof InsertToMongodb);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final UpdateParserMultiple request1 = new UpdateParserMultiple();
            request1.parse(exampleUpdateMd);
            assertNotNull(request1);
            assertTrue(RequestToMongodb.getRequestToMongoDb(request1) instanceof UpdateToMongodb);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final DeleteParserMultiple request1 = new DeleteParserMultiple();
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
            } catch (final IllegalAccessError e) {}
            assertNotNull(rtm.getRequest());
            assertNotNull(rtm.getNthQuery(0));
            assertNotNull(rtm.getRequestParser());
            assertNotNull(rtm.model());
            System.out.println("OrderBy = " + MongoDbHelper.bsonToString(rtm.getFinalOrderBy(), false));
            System.out.println("Projection = " + MongoDbHelper.bsonToString(rtm.getFinalProjection(), false));
            System.out.println(
                "Select Context = " +
                rtm.getLastDepth() +
                ":" +
                rtm.getFinalLimit() +
                ":" +
                rtm.getFinalOffset() +
                ":" +
                rtm.getUsage() +
                ":" +
                rtm.getHints()
            );
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
        final SelectParserMultiple request1 = new SelectParserMultiple();
        request1.parse(JsonHandler.getFromString(example1));
        final SelectToMongodb rtm1 = new SelectToMongodb(request1);
        assertEquals("", MongoDbHelper.bsonToString(rtm1.getFinalOrderBy(), false));

        final String example2 = "{ $roots : [], $query : [], $filter : {$orderby : {maclef : 1}}, $projection : {} }";
        final SelectParserMultiple request2 = new SelectParserMultiple();
        request2.parse(JsonHandler.getFromString(example2));
        final SelectToMongodb rtm2 = new SelectToMongodb(request2);
        assertEquals("{\"maclef\": 1}", MongoDbHelper.bsonToString(rtm2.getFinalOrderBy(), false));

        final String example3 = "{ $roots : [], $query : [], $filter : {$orderby : {maclef : -1}}, $projection : {} }";
        final SelectParserMultiple request3 = new SelectParserMultiple();
        request3.parse(JsonHandler.getFromString(example3));
        final SelectToMongodb rtm3 = new SelectToMongodb(request3);
        assertEquals("{\"maclef\": -1}", MongoDbHelper.bsonToString(rtm3.getFinalOrderBy(), false));
    }

    @Test
    public void testSelectGetFinalProjection() throws InvalidParseOperationException {
        final String example1 =
            "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : 1, #all : -1}} }";
        final SelectParserMultiple request1 = new SelectParserMultiple();
        request1.parse(JsonHandler.getFromString(example1));
        final SelectToMongodb rtm1 = new SelectToMongodb(request1);
        assertEquals(
            "{\"#dua\": 1, \"_id\": 1, \"#all\": 0}",
            MongoDbHelper.bsonToString(rtm1.getFinalProjection(), false)
        );

        final String example2 = "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : 1}} }";
        final SelectParserMultiple request2 = new SelectParserMultiple();
        request2.parse(JsonHandler.getFromString(example2));
        final SelectToMongodb rtm2 = new SelectToMongodb(request2);
        assertEquals("{\"#dua\": 1, \"_id\": 1}", MongoDbHelper.bsonToString(rtm2.getFinalProjection(), false));

        final String example3 = "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : -1}} }";
        final SelectParserMultiple request3 = new SelectParserMultiple();
        request3.parse(JsonHandler.getFromString(example3));
        final SelectToMongodb rtm3 = new SelectToMongodb(request3);
        assertEquals("{\"_id\": 1, \"#dua\": 0}", MongoDbHelper.bsonToString(rtm3.getFinalProjection(), false));

        final String example4 =
            "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : -1, " +
            "\"test1\" : {\"$slice\" : 1}}} }";
        final SelectParserMultiple request4 = new SelectParserMultiple();
        request4.parse(JsonHandler.getFromString(example4));
        final SelectToMongodb rtm4 = new SelectToMongodb(request4);
        assertEquals(
            "{\"_id\": 1, \"#dua\": 0, \"test1\": {\"$slice\": 1}}",
            MongoDbHelper.bsonToString(rtm4.getFinalProjection(), false)
        );

        final String example5 =
            "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : -1, " +
            "\"test1\" : {\"$slice\" : [0,1]}}} }";
        final SelectParserMultiple request5 = new SelectParserMultiple();
        request5.parse(JsonHandler.getFromString(example5));
        final SelectToMongodb rtm5 = new SelectToMongodb(request5);
        assertEquals(
            "{\"_id\": 1, \"#dua\": 0, \"test1\": {\"$slice\": [0, 1]}}",
            MongoDbHelper.bsonToString(rtm5.getFinalProjection(), false)
        );

        final String exampleEmptyProjection = "{ $roots : [], $query : [], $filter : {}, $projection : {$fields:{}} }";
        final SelectParserMultiple requestEmptyProjection = new SelectParserMultiple();
        requestEmptyProjection.parse(JsonHandler.getFromString(exampleEmptyProjection));
        final SelectToMongodb rtmEmptyProjection = new SelectToMongodb(requestEmptyProjection);
        assertNull(rtmEmptyProjection.getFinalProjection());

        try {
            // Test invalid slice projection
            final String example6 =
                "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : -1, " +
                "\"test1\" : {\"$slice\" : [0,0,1]}}} }";
            final SelectParserMultiple request6 = new SelectParserMultiple();
            request6.parse(JsonHandler.getFromString(example6));
            final SelectToMongodb rtm6 = new SelectToMongodb(request6);
            rtm6.getFinalProjection();
            fail("Invalid projection should, an exception should have been raised");
        } catch (final InvalidParseOperationException exc) {
            // DO NOTHING
        }

        try {
            // Test invalid slice projection
            final String example7 =
                "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : -1, " +
                "\"test1\" : {\"$slice\" : {\"field\":\"value\"}}}} }";
            final SelectParserMultiple request7 = new SelectParserMultiple();
            request7.parse(JsonHandler.getFromString(example7));
            final SelectToMongodb rtm7 = new SelectToMongodb(request7);
            rtm7.getFinalProjection();
            fail("Invalid projection should, an exception should have been raised");
        } catch (final InvalidParseOperationException exc) {
            // DO NOTHING
        }

        try {
            // Test invalid slice projection
            final String example7 =
                "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : -1, " +
                "\"test1\" : {\"$slice\" : [\"test\", \"s\"]}}} }";
            final SelectParserMultiple request7 = new SelectParserMultiple();
            request7.parse(JsonHandler.getFromString(example7));
            final SelectToMongodb rtm7 = new SelectToMongodb(request7);
            rtm7.getFinalProjection();
            fail("Invalid projection should, an exception should have been raised");
        } catch (final InvalidParseOperationException exc) {
            // DO NOTHING
        }

        try {
            // Test unsupported projection
            final String example8 =
                "{ $roots : [], $query : [], $filter : {}, $projection : {$fields : {#dua : -1, " +
                "\"test1\" : {\"field\":\"value\"}}} }";
            final SelectParserMultiple request8 = new SelectParserMultiple();
            request8.parse(JsonHandler.getFromString(example8));
            final SelectToMongodb rtm8 = new SelectToMongodb(request8);
            rtm8.getFinalProjection();
            fail("Invalid projection should, an exception should have been raised");
        } catch (final InvalidParseOperationException exc) {
            // DO NOTHING
        }
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
