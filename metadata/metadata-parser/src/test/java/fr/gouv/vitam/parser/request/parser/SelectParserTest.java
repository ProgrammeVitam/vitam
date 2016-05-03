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

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.builder.request.construct.Select;
import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTION;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.SELECTFILTER;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.parser.request.parser.SelectParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

@SuppressWarnings("javadoc")
public class SelectParserTest {
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
            + "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], "
            + "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } },"
            + "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' } }";

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
            + "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], "
            + "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } },"
            + "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' } }";

    @Before
    public void init() {
        VitamLoggerFactory.setLogLevel(VitamLogLevel.INFO);
    }

    private static String createLongString(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Test
    public void testSanityCheckRequest() {
        try {
            String longfalsecode = createLongString(GlobalDatasParser.limitRequest + 100);
            final SelectParser request1 = new SelectParser();
            request1.parse(longfalsecode);
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
        }
    }

    @Test
    public void testParse() {
        try {
            final SelectParser request1 = new SelectParser();
            request1.parse(exampleBothEsMd);
            assertTrue("Should refuse the request since ES is not allowed",
                    request1.hasFullTextQuery());
            request1.parse(exampleMd);
            assertFalse("Should accept the request since ES is not allowed",
                    request1.hasFullTextQuery());
        } catch (final Exception e) {
        }
        try {
            final SelectParser request1 = new SelectParser();
            request1.parse(exampleBothEsMd);
            assertNotNull(request1);
            assertTrue("Should refuse the request since ES is not allowed",
                    request1.hasFullTextQuery());
            final Select select = new Select();
            select.addRoots("id0");
            select.addQueries(path("id1", "id2"));
            select.addQueries(
                    and().add(exists("mavar1"), missing("mavar2"), isNull("mavar3"),
                            or().add(in("mavar4", 1, 2).add("maval1"),
                                    nin("mavar5", "maval2").add(true))));
            select.addQueries(
                    not().add(size("mavar5", 5), gt("mavar6", 7), lte("mavar7", 8))
                            .setExactDepthLimit(4));
            select.addQueries(not()
                    .add(eq("mavar8", 5), ne("mavar9", "ab"),
                            range("mavar10", 12, true, 20, true))
                    .setDepthLimit(1));
            select.addQueries(matchPhrase("mavar11", "ceci est une phrase")
                    .setRelativeDepthLimit(0));
            select.addQueries(matchPhrasePrefix("mavar11", "ceci est une phrase")
                    .setMatchMaxExpansions(10)
                    .setDepthLimit(0));
            select.addQueries(flt("ceci est une phrase", "mavar12", "mavar13")
                    .setRelativeDepthLimit(1));
            select.addQueries(and().add(search("mavar13", "ceci est une phrase"),
                    regex("mavar14", "^start?aa.*")));

            select.addQueries(and()
                    .add(term("mavar14", "motMajuscule").add("mavar15", "simplemot")));
            select.addQueries(
                    and().add(term("mavar16", "motMajuscule").add("mavar17", "simplemot"),
                            or().add(eq("mavar19", "abcd"),
                                    match("mavar18", "quelques mots"))));
            select.addQueries(regex("mavar14", "^start?aa.*"));

            select.setLimitFilter(100, 1000).addHintFilter(FILTERARGS.cache.exactToken());
            select.addOrderByAscFilter("maclef1")
                    .addOrderByDescFilter("maclef2").addOrderByAscFilter("maclef3");
            select.addUsedProjection("#dua", "#all").setUsageProjection("abcdef1234");
            final SelectParser request2 = new SelectParser();
            request2.parse(select.getFinalSelect().toString());
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
            assertTrue("Projection should be equal",
                    request1.getRequest().getProjection().toString()
                            .equals(request2.getRequest().getProjection().toString()));
            assertTrue("OrderBy should be equal",
                    request1.getRequest().getFilter().toString()
                            .equals(request2.getRequest().getFilter().toString()));
            assertEquals(request1.getLastDepth(), request2.getLastDepth());
            assertEquals(request1.hasFullTextQuery(), request2.hasFullTextQuery());
            assertEquals(request1.getRequest().getRoots().toString(),
                    request2.getRequest().getRoots().toString());
            assertEquals(request1.getRequest().getFinalSelect().toString(),
                    request2.getRequest().getFinalSelect().toString());
            assertTrue("Command should be equal",
                    request1.toString().equals(request2.toString()));
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFilterParse() {
        final SelectParser request = new SelectParser();
        final Select select = new Select();
        try {
            // empty
            request.filterParse(select.getFilter());
            assertNull("Hint should be null",
                    request.getRequest().getFilter().get(SELECTFILTER.hint.exactToken()));
            assertNotNull("Limit should not be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.limit.exactToken()));
            assertNull("Offset should be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.offset.exactToken()));
            assertNull("OrderBy should be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.orderby.exactToken()));
            // hint set
            select.addHintFilter(FILTERARGS.cache.exactToken());
            request.filterParse(select.getFilter());
            assertEquals("Hint should be True", FILTERARGS.cache.exactToken(),
                    request.getRequest().getFilter().get(SELECTFILTER.hint.exactToken())
                            .get(0).asText());
            // hint reset
            select.resetHintFilter();
            request.filterParse(select.getFilter());
            assertNull("Hint should be null",
                    request.getRequest().getFilter().get(SELECTFILTER.hint.exactToken()));
            // limit set
            select.setLimitFilter(0, 1000);
            request.filterParse(select.getFilter());
            assertEquals(1000,
                    request.getRequest().getFilter().get(SELECTFILTER.limit.exactToken())
                            .asLong());
            assertNull("Offset should be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.offset.exactToken()));
            // offset set
            select.setLimitFilter(100, 0);
            request.filterParse(select.getFilter());
            assertEquals(100,
                    request.getRequest().getFilter().get(SELECTFILTER.offset.exactToken())
                            .asLong());
            assertEquals(GlobalDatas.limitLoad,
                    request.getRequest().getFilter().get(SELECTFILTER.limit.exactToken())
                            .asLong());
            // orderBy set through array
            select.addOrderByAscFilter("var1", "var2").addOrderByDescFilter("var3");
            request.filterParse(select.getFilter());
            assertNotNull("OrderBy should not be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.orderby.exactToken()));
            // check both
            assertEquals(3, request.getRequest().getFilter()
                    .get(SELECTFILTER.orderby.exactToken()).size());
            for (final Iterator<Entry<String, JsonNode>> iterator =
                    request.getRequest().getFilter()
                            .get(SELECTFILTER.orderby.exactToken()).fields(); iterator
                                    .hasNext();) {
                final Entry<String, JsonNode> entry = iterator.next();
                if (entry.getKey().equals("var1")) {
                    assertEquals(1, entry.getValue().asInt());
                }
                if (entry.getKey().equals("var2")) {
                    assertEquals(1, entry.getValue().asInt());
                }
                if (entry.getKey().equals("var3")) {
                    assertEquals(-1, entry.getValue().asInt());
                }
            }
            // orderBy set through composite
            select.resetOrderByFilter();
            request.filterParse(select.getFilter());
            assertNull("OrderBy should be null", request.getRequest().getFilter()
                    .get(SELECTFILTER.orderby.exactToken()));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testProjectionParse() {
        final SelectParser request = new SelectParser();
        final Select select = new Select();
        try {
            // empty rootNode
            request.projectionParse(select.getProjection());
            assertEquals("Projection should be empty", 0,
                    request.getRequest().getProjection().size());
            // contractId set
            select.setUsageProjection("abcd");
            request.projectionParse(select.getProjection());
            assertNotNull("Projection Usage should not be empty", request.getRequest()
                    .getProjection().get(PROJECTION.usage.exactToken()));
            // projection set but empty
            select.addUsedProjection((String) null);
            // empty set
            request.projectionParse(select.getProjection());
            assertNotNull("Projection Usage should not be be empty", request.getRequest()
                    .getProjection().get(PROJECTION.usage.exactToken()));
            assertEquals("Projection should not be empty", 1,
                    request.getRequest().getProjection().size());
            // projection set 
            select.addUsedProjection("var");
            // empty set
            request.projectionParse(select.getProjection());
            assertNotNull("Projection Usage should not be be empty", request.getRequest()
                    .getProjection().get(PROJECTION.usage.exactToken()));
            assertEquals("Projection should not be empty", 2,
                    request.getRequest().getProjection().size());
            // reset
            select.resetUsageProjection().resetUsedProjection();
            request.projectionParse(select.getProjection());
            assertNull("Projection Usage should be empty", request.getRequest()
                    .getProjection().get(PROJECTION.usage.exactToken()));
            assertEquals("Projection should be empty", 0,
                    request.getRequest().getProjection().size());
            // not empty set
            select.addUsedProjection("var1").addUnusedProjection("var2");
            request.projectionParse(select.getProjection());
            assertEquals("Projection should not be empty", 1,
                    request.getRequest().getProjection().size());
            assertEquals(2, request.getRequest().getProjection()
                    .get(PROJECTION.fields.exactToken()).size());
            for (final Iterator<Entry<String, JsonNode>> iterator =
                    request.getRequest().getProjection()
                            .get(PROJECTION.fields.exactToken()).fields(); iterator
                                    .hasNext();) {
                final Entry<String, JsonNode> entry = iterator.next();
                if (entry.getKey().equals("var1")) {
                    assertEquals(1, entry.getValue().asInt());
                }
                if (entry.getKey().equals("var2")) {
                    assertEquals(0, entry.getValue().asInt());
                }
            }
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
