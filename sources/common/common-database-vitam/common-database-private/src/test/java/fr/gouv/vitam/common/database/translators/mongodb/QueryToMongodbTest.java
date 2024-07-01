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
import fr.gouv.vitam.common.database.builder.query.MltQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.SearchQuery;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.bson.conversions.Bson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.mlt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.search;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class QueryToMongodbTest {

    private static final String exampleMd =
        "{ $roots : [ 'id0' ], $query : [ " +
        "{ $path : [ 'id1', 'id2'] }," +
        "{ $and : [ " +
        "{$exists : 'mavar1'}, " +
        "{$missing : 'mavar2'}, " +
        "{$isNull : 'mavar3'}, " +
        "{ $or : [ " +
        "{$in : { 'mavar4' : [1, 2, 'maval1'] }}, " +
        "{ $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ " +
        "{ $size : { 'mavar5' : 5 } }, " +
        "{ $gt : { 'mavar6' : 7 } }, " +
        "{ $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
        "{ $not : [ " +
        "{ $eq : { 'mavar8' : 5 } }, " +
        "{ $ne : { 'mavar9' : 'ab' } }, " +
        "{ $wildcard : { 'mavar9' : 'ab' } }, " +
        "{ $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " +
        "], " +
        "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
        "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
        "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' } }";

    private static final String multiRoots = "{ $roots: ['id0', 'id1'], $query : [], $filter : [], $projection : [] }";
    private static final String wildcard =
        "{ $roots: [], $query : [{ $wildcard : { 'mavar14' : 'motMajuscule'}}], $filter : [], $projection : [] }";
    private static final String EMPTY_QUERY = "{ $roots: [], $query : {}, $filter : [], $projection : [] }";
    private static JsonNode example;
    private static JsonNode multiRootsJson;
    private static JsonNode wildcardJson;
    private static JsonNode emptyQueryJson;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        example = JsonHandler.getFromString(exampleMd);
        multiRootsJson = JsonHandler.getFromString(multiRoots);
        wildcardJson = JsonHandler.getFromString(wildcard);
        emptyQueryJson = JsonHandler.getFromString(EMPTY_QUERY);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    private SelectMultiQuery createSelect() {
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(example);
            assertNotNull(request1);
            return request1.getRequest();
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void testGetCommands() {
        try {
            final SelectMultiQuery select = createSelect();
            final Bson bsonRoot = QueryToMongodb.getRoots("_up", select.getRoots());
            final List<Query> list = select.getQueries();
            for (int i = 0; i < list.size(); i++) {
                System.out.println(i + " = " + list.get(i).toString());
                final Bson bsonQuery = QueryToMongodb.getCommand(list.get(i));
                final Bson pseudoRequest = QueryToMongodb.getFullCommand(bsonQuery, bsonRoot);
                System.out.println(i + " = " + MongoDbHelper.bsonToString(pseudoRequest, false));
            }
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testNopCommand() {
        final SelectParserSingle request = new SelectParserSingle();
        try {
            request.parse(emptyQueryJson);
            final fr.gouv.vitam.common.database.builder.request.single.Select select = request.getRequest();
            final Bson command = QueryToMongodb.getCommand(select.getQuery());
            assertEquals("{}", command.toString());
        } catch (final InvalidParseOperationException e) {
            fail("No exception should be thrown here");
        }
    }

    @Test
    public void testGetMultiRoots() throws InvalidParseOperationException {
        final SelectParserMultiple request = new SelectParserMultiple();
        request.parse(multiRootsJson);
        final SelectMultiQuery select = request.getRequest();
        final Bson bsonRoot = QueryToMongodb.getRoots("_up", select.getRoots());
        assertEquals("{\"_up\": {\"$in\": [\"id0\", \"id1\"]}}", MongoDbHelper.bsonToString(bsonRoot, false));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void shouldRaiseException_whenMltIsNotAllowed()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Query query = new MltQuery(QUERY.MLT, "var", "val");
        QueryToMongodb.getCommand(query);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void shouldRaiseException_whenSearchIsNotAllowed()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Query query = new SearchQuery(QUERY.SEARCH, "var", "val");
        QueryToMongodb.getCommand(query);
    }

    @Test
    public void testWildcardCase() throws InvalidParseOperationException {
        final SelectParserMultiple request = new SelectParserMultiple();
        request.parse(wildcardJson);
        final SelectMultiQuery select = request.getRequest();
        final List<Query> list = select.getQueries();
        final Bson bsonQuery = QueryToMongodb.getCommand(list.get(0));
        assertEquals(
            "{\"mavar14\": {\"$regex\": \"motMajuscule\", \"$options\": \"\"}}",
            MongoDbHelper.bsonToString(bsonQuery, false)
        );
    }

    @Test(expected = InvalidParseOperationException.class)
    public void testGetCommandsThrowInvalidParseOperationExceptionWithMLT()
        throws InvalidCreateOperationException, InvalidParseOperationException {
        QueryToMongodb.getCommand(mlt("value", "var1", "var2"));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void testGetCommandsThrowInvalidParseOperationExceptionWithSEARCH()
        throws InvalidCreateOperationException, InvalidParseOperationException {
        QueryToMongodb.getCommand(search("var1", "var2"));
    }
}
