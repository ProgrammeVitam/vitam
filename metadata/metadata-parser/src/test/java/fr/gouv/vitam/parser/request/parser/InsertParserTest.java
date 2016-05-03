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
package fr.gouv.vitam.parser.request.parser;

import static fr.gouv.vitam.builder.request.construct.QueryHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.builder.request.construct.Insert;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.MULTIFILTER;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.SELECTFILTER;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

@SuppressWarnings("javadoc")
public class InsertParserTest {
    private static final String data = "{ "
            + "\"address\": { \"streetAddress\": \"21 2nd Street\", \"city\": \"New York\" }, "
            + "\"phoneNumber\": [ { \"location\": \"home\", \"code\": 44 } ] }";

    private static final String exampleBothEsMd = "{ $roots : [ 'id0' ], $query : [ "
            + "{ $path : [ 'id1', 'id2'] },"
            + "{ $and : [ "
            + "{$exists : 'mavar1'}, "
            + "{$missing : 'mavar2'}, "
            + "{$isNull : 'mavar3'}, "
            + "{ $or : [ "
            + "{$in : { 'mavar4' : [1, 2, 'maval1'] }}, "
            + "{ $nin : { 'mavar5' : ['maval2', true] } } ] } ] },"
            + "{ $not : [ "
            + "{ $size : { 'mavar5' : 5 } }, "
            + "{ $gt : { 'mavar6' : 7 } }, "
            + "{ $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4},"
            + "{ $not : [ "
            + "{ $eq : { 'mavar8' : 5 } }, { "
            + "$ne : { 'mavar9' : 'ab' } }, { "
            + "$range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1},"
            + "{ $match_phrase : { 'mavar11' : 'ceci est une phrase' }, $depth : 0},"
            + "{ $match_phrase_prefix : { 'mavar11' : 'ceci est une phrase', $max_expansions : 10 }, $depth : 0},"
            + "{ $flt : { $fields : [ 'mavar12', 'mavar13' ], $like : 'ceci est une phrase' }, $depth : 1},"
            + "{ $and : [ "
            + "{ $search : { 'mavar13' : 'ceci est une phrase' } }, "
            + "{ $regex : { 'mavar14' : '^start?aa.*' } } ] },"
            + "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, "
            + "{ $and : [ "
            + "{ $term : { 'mavar16' : 'motMajuscule', 'mavar17' : 'simplemot' } }, "
            + "{ $or : [ {$eq : { 'mavar19' : 'abcd' } }, { $match : { 'mavar18' : 'quelques mots' } } ] } ] }, "
            + "{ $regex : { 'mavar14' : '^start?aa.*' } } "
            + "], "
            + "$filter : {$mult : false },"
            + "$data : " + data + " }";

    private static final String exampleMd = "{ $roots : [ 'id0' ], $query : [ "
            + "{ $path : [ 'id1', 'id2'] },"
            + "{ $and : [ "
            + "{$exists : 'mavar1'}, "
            + "{$missing : 'mavar2'}, "
            + "{$isNull : 'mavar3'}, "
            + "{ $or : [ "
            + "{$in : { 'mavar4' : [1, 2, 'maval1'] }}, "
            + "{ $nin : { 'mavar5' : ['maval2', true] } } ] } ] },"
            + "{ $not : [ "
            + "{ $size : { 'mavar5' : 5 } }, "
            + "{ $gt : { 'mavar6' : 7 } }, "
            + "{ $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4},"
            + "{ $not : [ "
            + "{ $eq : { 'mavar8' : 5 } }, "
            + "{ $ne : { 'mavar9' : 'ab' } }, "
            + "{ $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, "
            + "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, "
            + "{ $regex : { 'mavar14' : '^start?aa.*' } } "
            + "], "
            + "$filter : {$mult : false },"
            + "$data : " + data + " }";

    @Before
    public void init() {
        VitamLoggerFactory.setLogLevel(VitamLogLevel.INFO);
    }

    @Test
    public void testParse() {
        try {
            final InsertParser request1 = new InsertParser();
            request1.parse(exampleBothEsMd);
            assertTrue("Should refuse the request since ES is not allowed",
                    request1.hasFullTextQuery());
            request1.parse(exampleMd);
            assertFalse("Should accept the request since ES is not allowed",
                    request1.hasFullTextQuery());
        } catch (final Exception e) {
        }
        try {
            final InsertParser request1 = new InsertParser();
            request1.parse(exampleBothEsMd);
            assertNotNull(request1);
            assertTrue("Should refuse the request since ES is not allowed",
                    request1.hasFullTextQuery());
            final Insert insert = new Insert();
            insert.addRoots("id0");
            insert.addQueries(path("id1", "id2"));
            insert.addQueries(
                    and().add(exists("mavar1"), missing("mavar2"), isNull("mavar3"),
                            or().add(in("mavar4", 1, 2).add("maval1"),
                                    nin("mavar5", "maval2").add(true))));
            insert.addQueries(
                    not().add(size("mavar5", 5), gt("mavar6", 7), lte("mavar7", 8))
                            .setExactDepthLimit(4));
            insert.addQueries(not()
                    .add(eq("mavar8", 5), ne("mavar9", "ab"),
                            range("mavar10", 12, true, 20, true))
                    .setDepthLimit(1));
            insert.addQueries(matchPhrase("mavar11", "ceci est une phrase")
                    .setRelativeDepthLimit(0));
            insert.addQueries(matchPhrasePrefix("mavar11", "ceci est une phrase")
                    .setMatchMaxExpansions(10)
                    .setDepthLimit(0));
            insert.addQueries(flt("ceci est une phrase", "mavar12", "mavar13")
                    .setRelativeDepthLimit(1));
            insert.addQueries(and().add(search("mavar13", "ceci est une phrase"),
                    regex("mavar14", "^start?aa.*")));
            insert.addQueries(and()
                    .add(term("mavar14", "motMajuscule").add("mavar15", "simplemot")));
            insert.addQueries(
                    and().add(term("mavar16", "motMajuscule").add("mavar17", "simplemot"),
                            or().add(eq("mavar19", "abcd"),
                                    match("mavar18", "quelques mots"))));
            insert.addQueries(regex("mavar14", "^start?aa.*"));

            insert.setMult(false);
            JsonNode node = JsonHandler.getFromString(data);
            insert.setData(node);

            final InsertParser request2 = new InsertParser();
            request2.parse(insert.getFinalInsert().toString());
            assertNotNull(request2);
            final List<Query> query1 = request1.getRequest().getQueries();
            final List<Query> query2 = request2.getRequest().getQueries();
            for (int i = 0; i < query1.size(); i++) {
                if (!query1.get(i).toString().equals(query2.get(i).toString())) {
                    System.err.println(query1.get(i));
                    System.err.println(query2.get(i));
                }
                assertTrue("TypeRequest should be equal",
                        query1.get(i).toString().equals(query2.get(i).toString()));
            }
            assertTrue("Data should be equal", request1.getRequest().getData().toString()
                    .equals(request2.getRequest().getData().toString()));
            assertTrue("OrderBy should be equal",
                    request1.getRequest().getFilter().toString()
                            .equals(request2.getRequest().getFilter().toString()));
            assertEquals(request1.getLastDepth(), request2.getLastDepth());
            assertEquals(request1.hasFullTextQuery(), request2.hasFullTextQuery());
            assertEquals(request1.getRequest().getRoots().toString(),
                    request2.getRequest().getRoots().toString());
            assertEquals(request1.getRequest().getFinalInsert().toString(),
                    request2.getRequest().getFinalInsert().toString());
            assertTrue("Command should be equal",
                    request1.toString().equals(request2.toString()));
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFilterParse() {
        final InsertParser request = new InsertParser();
        final Insert insert = new Insert();
        try {
            // empty
            request.filterParse(insert.getFilter());
            assertNull("Hint should be null",
                    request.getRequest().getFilter().get(SELECTFILTER.hint.exactToken()));
            assertNull("Limit should be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.limit.exactToken()));
            assertNull("Offset should be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.offset.exactToken()));
            assertNull("OrderBy should be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.orderby.exactToken()));
            assertNull("Mult should be null",
                    request.getRequest().getFilter().get(MULTIFILTER.mult.exactToken()));
            // hint set
            insert.addHintFilter(FILTERARGS.cache.exactToken());
            request.filterParse(insert.getFilter());
            assertEquals("Hint should be True", FILTERARGS.cache.exactToken(),
                    request.getRequest().getFilter().get(SELECTFILTER.hint.exactToken())
                            .get(0).asText());
            // hint reset
            insert.resetHintFilter();
            request.filterParse(insert.getFilter());
            assertNull("Hint should be null",
                    request.getRequest().getFilter().get(SELECTFILTER.hint.exactToken()));
            // multi set
            insert.setMult(false);
            request.filterParse(insert.getFilter());
            assertEquals(false,
                    request.getRequest().getFilter().get(MULTIFILTER.mult.exactToken())
                            .asBoolean());
            insert.setMult(true);
            request.filterParse(insert.getFilter());
            assertEquals(true,
                    request.getRequest().getFilter().get(MULTIFILTER.mult.exactToken())
                            .asBoolean());
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testDataParse() {
        final InsertParser request = new InsertParser();
        final Insert insert = new Insert();
        try {
            // empty rootNode
            request.dataParse(insert.getData());
            assertEquals("Data should be empty", 0,
                    request.getRequest().getData().size());
            JsonNode node = JsonHandler.getFromString(data);
            request.dataParse(node);
            insert.parseData(data);
            assertEquals(request.getRequest().getFinalInsert().toString(),
                    insert.getFinalInsert().toString());
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        JsonNode node;
        try {
            node = JsonHandler.getFromString("{#id: \"value\"}");
        } catch (InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
            return;
        }
        try {
            request.dataParse(node);
            fail("Should Failed");
        } catch (InvalidParseOperationException e) {
            // Should failed
        }
        
    }
}
