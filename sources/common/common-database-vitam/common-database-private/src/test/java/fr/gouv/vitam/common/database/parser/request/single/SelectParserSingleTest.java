package fr.gouv.vitam.common.database.parser.request.single;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.flt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.isNull;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.matchPhrase;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.matchPhrasePrefix;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.missing;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.mlt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nin;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.not;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.path;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.prefix;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.range;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.regex;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.search;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.size;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.term;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.wildcard;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

public class SelectParserSingleTest {
    private static JsonNode EX_BOTH_ES_MD;

    private static JsonNode EX_MD;

    private static JsonNode EX_MD2;
    static final ObjectNode DEFAULT_SLICE = JsonHandler.createObjectNode();
    static final ObjectNode DEFAULT_ALLKEYS = JsonHandler.createObjectNode();

    @BeforeClass
    public static void init() throws InvalidParseOperationException {
        VitamLoggerFactory.setLogLevel(VitamLogLevel.INFO);
        EX_MD2 = JsonHandler.getFromString("{ $query : " + "{ $and : [ " + "{$exists : 'mavar1'}, " +
            "{$missing : 'mavar2'}, " + "{$isNull : 'mavar3'}, " + "{ $or : [ " +
            "{$in : { 'mavar4' : [1, 2, 'maval1'] } }, " + "{ $nin : { 'mavar5' : ['maval2', true] } } ] }," +
            "{ $not : [ " + "{ $size : { 'mavar5' : 5 } }, " + "{ $gt : { 'mavar6' : 7 } }, " +
            "{ $lte : { 'mavar7' : 8 } } ] }," + "{ $not : [ " + "{ $eq : { 'mavar8' : 5 } }, { " +
            "$ne : { 'mavar9' : 'ab' } }, { " + "$range : { 'mavar10' : { $gte : 12, $lte : 20} } } ] }," +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' } } " + "] } , " +
            "$filter : { " +
            "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
            "$projection : {$fields : {#dua : 1, myvar : 1} } }");
        
        EX_MD = JsonHandler.getFromString("{ $query : " + "{ $and : [ " + "{$exists : 'mavar1'}, " +
            "{$missing : 'mavar2'}, " + "{$isNull : 'mavar3'}, " + "{ $or : [ " +
            "{$in : { 'mavar4' : [1, 2, 'maval1'] } }, " + "{ $nin : { 'mavar5' : ['maval2', true] } } ] }," +
            "{ $not : [ " + "{ $size : { 'mavar5' : 5 } }, " + "{ $gt : { 'mavar6' : 7 } }, " +
            "{ $lte : { 'mavar7' : 8 } } ] }," + "{ $not : [ " + "{ $eq : { 'mavar8' : 5 } }, { " +
            "$ne : { 'mavar9' : 'ab' } }, { " + "$range : { 'mavar10' : { $gte : 12, $lte : 20} } } ] }," +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' } } " + "] } , " +
            "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
            "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
            "$projection : {$fields : {#dua : 1, #all : 1} } }");
        
        EX_BOTH_ES_MD = JsonHandler.getFromString("{ $query : " + "{ $and : [ " + "{$exists : 'mavar1'}, " +
            "{$missing : 'mavar2'}, " + "{$isNull : 'mavar3'}, " + "{ $or : [ " +
            "{$in : { 'mavar4' : [1, 2, 'maval1'] } }, " + "{ $nin : { 'mavar5' : ['maval2', true] } } ] }," +
            "{ $not : [ " + "{ $size : { 'mavar5' : 5 } }, " + "{ $gt : { 'mavar6' : 7 } }, " +
            "{ $lte : { 'mavar7' : 8 } },  { $gte : { 'mavar7' : 8 } }, { $lt : { 'mavar7' : 8 } } ] }," + "{ $not : [ " +
            "{ $eq : { 'mavar8' : 5 } }, { " +
            "$ne : { 'mavar9' : 'ab' } }, { " + "$range : { 'mavar10' : { $gte : 12, $lte : 20} } } ] }," +
            "{ $match_phrase : { 'mavar11' : 'ceci est une phrase' } }," +
            "{ $match_phrase_prefix : { 'mavar11' : 'ceci est une phrase', $max_expansions : 10 } }," +
            "{ $flt : { $fields : [ 'mavar12', 'mavar13' ], $like : 'ceci est une phrase' } }," +
            "{ $mlt : { $fields : [ 'mavar12', 'mavar13' ], $like : 'ceci est une phrase' } }," +
            "{ $and : [ " +
            "{ $search : { 'mavar13' : 'ceci est une phrase' } }, " +
            "{ $prefix : { 'mavar13' : 'ceci est une phrase' } }, " +
            "{ $wildcard : { 'mavar13' : 'ceci' } }, " + "{ $regex : { 'mavar14' : '^start?aa.*' } } ] }," +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " + "{ $and : [ " +
            "{ $term : { 'mavar16' : 'motMajuscule', 'mavar17' : 'simplemot' } }, " +
            "{ $or : [ {$eq : { 'mavar19' : 'abcd' } }, { $match : { 'mavar18' : 'quelques mots' } } ] } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' } } " + "] } , " +
            "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
            "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
            "$projection : {$fields : {#dua : 1, #all : 1} } }");
        
        
    }

    private static String createLongString(int size) {
        final StringBuilder sb = new StringBuilder(size);
        sb.append("{a:");
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        sb.append("}");
        return sb.toString();
    }

    @Test
    public void testSanityCheckRequest() {
        final int previous = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 100;
        try {
            final String longfalsecode = createLongString(GlobalDatasParser.limitRequest + 100);
            final SelectParserSingle request1 = new SelectParserSingle();
            request1.parse(JsonHandler.getFromString(longfalsecode));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // ignore
        } finally {
            GlobalDatasParser.limitRequest = previous;
        }
    }

    @Test
    public void testParse() {
        try {
            final SelectParserSingle request1 = new SelectParserSingle();
            request1.parse(EX_BOTH_ES_MD);
            assertTrue("Should refuse the request since ES is not allowed",
                request1.hasFullTextQuery());
            request1.parse(EX_MD);
            assertFalse("Should accept the request since ES is not allowed",
                request1.hasFullTextQuery());
        } catch (final Exception e) {}
        try {
            final SelectParserSingle request1 = new SelectParserSingle();
            request1.parse(EX_BOTH_ES_MD);
            assertNotNull(request1);
            assertTrue("Should refuse the request since ES is not allowed",
                request1.hasFullTextQuery());
            final Select select = new Select();
            select.setQuery(path("id1"));
            select.setQuery(
                and().add(exists("mavar1"), missing("mavar2"), isNull("mavar3"),
                    or().add(in("mavar4", 1, 2).add("maval1"),
                        nin("mavar5", "maval2").add(true)),
                    not().add(size("mavar5", 5), gt("mavar6", 7), lte("mavar7", 8),
                        gte("mavar7", 8), lt("mavar7", 8)),
                    not().add(eq("mavar8", 5), ne("mavar9", "ab"),
                        range("mavar10", 12, true, 20, true)),
                    matchPhrase("mavar11", "ceci est une phrase"),
                    matchPhrasePrefix("mavar11", "ceci est une phrase")
                        .setMatchMaxExpansions(10),
                    flt("ceci est une phrase", "mavar12", "mavar13"),
                    mlt("ceci est une phrase", "mavar12", "mavar13"),
                    and().add(search("mavar13", "ceci est une phrase"),
                        prefix("mavar13", "ceci est une phrase"),
                        wildcard("mavar13", "ceci"),
                        regex("mavar14", "^start?aa.*")),
                    and().add(term("mavar14", "motMajuscule").add("mavar15", "simplemot")),
                    and().add(term("mavar16", "motMajuscule").add("mavar17", "simplemot"),
                        or().add(eq("mavar19", "abcd"),
                            match("mavar18", "quelques mots"))),
                    regex("mavar14", "^start?aa.*")));
            select.setLimitFilter(100, 1000).addHintFilter(FILTERARGS.CACHE.exactToken());
            select.addOrderByAscFilter("maclef1")
                .addOrderByDescFilter("maclef2").addOrderByAscFilter("maclef3");
            select.addUsedProjection("#dua", "#all");
            final SelectParserSingle request2 = new SelectParserSingle();
            request2.parse(select.getFinalSelect());
            assertNotNull(request2);
            final Query query1 = request1.getRequest().getQuery();
            final Query query2 = request2.getRequest().getQuery();
            if (!query1.toString().equals(query2.toString())) {
                System.err.println(query1);
                System.err.println(query2);
            }
            assertTrue("TypeRequest should be equal",
                query1.toString().equals(query2.toString()));
            assertTrue("Projection should be equal",
                request1.getRequest().getProjection().toString()
                    .equals(request2.getRequest().getProjection().toString()));
            assertTrue("OrderBy should be equal",
                request1.getRequest().getFilter().toString()
                    .equals(request2.getRequest().getFilter().toString()));
            assertEquals(request1.hasFullTextQuery(), request2.hasFullTextQuery());
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
        final SelectParserSingle request = new SelectParserSingle();
        final Select select = new Select();
        try {
            // empty
            request.filterParse(select.getFilter());
            assertNull("Hint should be null",
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            assertNotNull("Limit should not be null", request.getRequest().getFilter()
                .get(SELECTFILTER.LIMIT.exactToken()));
            assertNull("Offset should be null", request.getRequest().getFilter()
                .get(SELECTFILTER.OFFSET.exactToken()));
            assertNull("OrderBy should be null", request.getRequest().getFilter()
                .get(SELECTFILTER.ORDERBY.exactToken()));
            // hint set
            select.addHintFilter(FILTERARGS.CACHE.exactToken());
            request.filterParse(select.getFilter());
            assertEquals("Hint should be True", FILTERARGS.CACHE.exactToken(),
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken())
                    .get(0).asText());
            // hint reset
            select.resetHintFilter();
            request.filterParse(select.getFilter());
            assertNull("Hint should be null",
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            // limit set
            select.setLimitFilter(0, 1000);
            request.filterParse(select.getFilter());
            assertEquals(1000,
                request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken())
                    .asLong());
            assertNull("Offset should be null", request.getRequest().getFilter()
                .get(SELECTFILTER.OFFSET.exactToken()));
            // offset set
            select.setLimitFilter(100, 0);
            request.filterParse(select.getFilter());
            assertEquals(100,
                request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken())
                    .asLong());
            assertEquals(GlobalDatas.LIMIT_LOAD,
                request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken())
                    .asLong());
            // orderBy set through array
            select.addOrderByAscFilter("var1", "var2").addOrderByDescFilter("var3");
            request.filterParse(select.getFilter());
            assertNotNull("OrderBy should not be null", request.getRequest().getFilter()
                .get(SELECTFILTER.ORDERBY.exactToken()));
            // check both
            assertEquals(3, request.getRequest().getFilter()
                .get(SELECTFILTER.ORDERBY.exactToken()).size());
            for (final Iterator<Entry<String, JsonNode>> iterator =
                request.getRequest().getFilter()
                    .get(SELECTFILTER.ORDERBY.exactToken()).fields(); iterator
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
                .get(SELECTFILTER.ORDERBY.exactToken()));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testProjectionParse() {
        final SelectParserSingle request = new SelectParserSingle();
        final Select select = new Select();
        try {
            // empty rootNode
            request.projectionParse(select.getProjection());
            assertEquals("Projection should be empty", 0,
                request.getRequest().getProjection().size());
            // projection set but empty
            select.addUsedProjection((String) null);
            // empty set
            request.projectionParse(select.getProjection());
            assertEquals("Projection should not be empty", 0,
                request.getRequest().getProjection().size());
            // projection set
            select.addUsedProjection("var");
            // empty set
            request.projectionParse(select.getProjection());
            assertEquals("Projection should not be empty", 1,
                request.getRequest().getProjection().size());
            // reset
            select.resetUsageProjection().resetUsedProjection();
            request.projectionParse(select.getProjection());
            assertEquals("Projection should be empty", 0,
                request.getRequest().getProjection().size());
            // not empty set
            select.addUsedProjection("var1").addUnusedProjection("var2");
            request.projectionParse(select.getProjection());
            assertEquals("Projection should not be empty", 1,
                request.getRequest().getProjection().size());
            assertEquals(2, request.getRequest().getProjection()
                .get(PROJECTION.FIELDS.exactToken()).size());
            for (final Iterator<Entry<String, JsonNode>> iterator =
                request.getRequest().getProjection()
                    .get(PROJECTION.FIELDS.exactToken()).fields(); iterator
                        .hasNext();) {
                final Entry<String, JsonNode> entry = iterator.next();
                if (entry.getKey().equals("var1")) {
                    assertEquals(1, entry.getValue().asInt());
                }
                if (entry.getKey().equals("var2")) {
                    assertEquals(0, entry.getValue().asInt());
                }
            }
            try {
                request.projectionParse(null);
            } catch (final InvalidParseOperationException e) {
                fail("Should not raized an exception");
            }
            try {
                request.addProjection(DEFAULT_SLICE, null);
                fail("Should raized an exception");
            } catch (final InvalidParseOperationException e) {
                // Ignore
            }
            try {
                request.addProjection(null, DEFAULT_ALLKEYS);
                fail("Should raized an exception");
            } catch (final InvalidParseOperationException e) {
                // Ignore
            }
            new SelectToMongoDb(request).getFinalProjection();
            try {
                request.addProjection(DEFAULT_SLICE, DEFAULT_ALLKEYS);
                new SelectToMongoDb(request).getFinalProjection();
            } catch (final InvalidParseOperationException e) {
                fail("Should not raized an exception");
            }
            request.projectionParse(JsonHandler.getFromString("{$fields: { var1: -1 }}"));
            new SelectToMongoDb(request).getFinalProjection();
            try {
                request.addProjection(DEFAULT_SLICE, DEFAULT_ALLKEYS);
                new SelectToMongoDb(request).getFinalProjection();
            } catch (final InvalidParseOperationException e) {
                fail("Should not raized an exception");
            }
            request.projectionParse(JsonHandler.getFromString("{}"));
            new SelectToMongoDb(request).getFinalProjection();
            try {
                request.addProjection(DEFAULT_SLICE, DEFAULT_ALLKEYS);
                new SelectToMongoDb(request).getFinalProjection();
            } catch (final InvalidParseOperationException e) {
                fail("Should not raized an exception");
            }
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testParseQueryOnly() throws InvalidParseOperationException {
        final SelectParserSingle request = new SelectParserSingle();
        final String ex = "{}";
        request.parseQueryOnly(ex);
        assertNotNull(request);
    }

    @Test
    public void testInternalParseSelect() throws InvalidParseOperationException {
        final SelectParserSingle request = new SelectParserSingle();
        final String s = "[ { $path : [ 'id1', 'id2'] }, {$mult : false }, {} ]";
        request.parse(JsonHandler.getFromString(s));
        assertNotNull(request);
    }

    @Test
    public void testToMongoDb() throws InvalidParseOperationException {
        final SelectParserSingle request = new SelectParserSingle();
        request.parse(EX_MD);
        assertNotNull(request);
        SelectToMongoDb selectToMongoDb = new SelectToMongoDb(request);
        assertFalse(selectToMongoDb.hasFullTextQuery());
        assertFalse(selectToMongoDb.hintNoTimeout());
        assertTrue(selectToMongoDb.getHints().size() > 0);
        assertTrue(selectToMongoDb.getFinalOffset() > 0);
        assertNotNull(selectToMongoDb.getFinalOrderBy());
        assertNull(selectToMongoDb.getFinalProjection());

        request.parse(EX_BOTH_ES_MD);
        selectToMongoDb = new SelectToMongoDb(request);
        assertTrue(selectToMongoDb.hasFullTextQuery());
        assertFalse(selectToMongoDb.hintNoTimeout());
        assertTrue(selectToMongoDb.getHints().size() > 0);
        assertTrue(selectToMongoDb.getFinalOffset() > 0);
        assertNotNull(selectToMongoDb.getFinalOrderBy());
        assertNull(selectToMongoDb.getFinalProjection());

        request.parse(EX_MD2);
        selectToMongoDb = new SelectToMongoDb(request);
        assertFalse(selectToMongoDb.hasFullTextQuery());
        assertFalse(selectToMongoDb.hintNoTimeout());
        assertNull(selectToMongoDb.getHints());
        assertTrue(selectToMongoDb.getFinalOffset() == 0);
        assertNotNull(selectToMongoDb.getFinalOrderBy());
        assertNotNull(selectToMongoDb.getFinalProjection());
    }

}
