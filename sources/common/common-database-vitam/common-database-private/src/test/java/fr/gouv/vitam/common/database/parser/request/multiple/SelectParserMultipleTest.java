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
package fr.gouv.vitam.common.database.parser.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.facet.FacetHelper;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.facet.model.FacetOrder;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static fr.gouv.vitam.common.database.builder.facet.FacetHelper.filters;
import static fr.gouv.vitam.common.database.builder.facet.FacetHelper.terms;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.flt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.isNull;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.matchPhrase;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.matchPhrasePrefix;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.missing;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nin;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.not;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.path;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.range;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.regex;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.search;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.size;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.term;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SelectParserMultipleTest {

    private static JsonNode exampleBothEsMd;

    private static JsonNode exampleMd;

    private static JsonNode nestedSearchQuery;

    @BeforeClass
    public static void init() throws InvalidParseOperationException {
        VitamLoggerFactory.setLogLevel(VitamLogLevel.INFO);
        exampleBothEsMd = JsonHandler.getFromString(
            "{\"$roots\":[\"id0\"],\"$query\":[{\"$path\":[\"id1\",\"id2\"]}," +
            "{\"$and\":[{\"$exists\":\"mavar1\"},{\"$missing\":\"mavar2\"},{\"$isNull\":\"mavar3\"}," +
            "{\"$or\":[{\"$in\":{\"mavar4\":[1,2,\"maval1\"]}},{\"$nin\":{\"mavar5\":[\"maval2\",true]}}]}]}," +
            "{\"$not\":[{\"$size\":{\"mavar5\":5}},{\"$gt\":{\"mavar6\":7}},{\"$lte\":{\"mavar7\":8}}],\"$exactdepth\":4}," +
            "{\"$not\":[{\"$eq\":{\"mavar8\":5}},{\"$ne\":{\"mavar9\":\"ab\"}}," +
            "{\"$range\":{\"mavar10\":{\"$gte\":12,\"$lte\":20}}}],\"$depth\":10}," +
            "{\"$match_phrase\":{\"mavar11\":\"ceci est une phrase\"},\"$depth\":0}," +
            "{\"$match_phrase_prefix\":{\"mavar11\":\"ceci est une phrase\",\"$max_expansions\":10},\"$depth\":0}," +
            "{\"$flt\":{\"$fields\":[\"mavar12\",\"mavar13\"],\"$like\":\"ceci est une phrase\"},\"$depth\":1}," +
            "{\"$and\":[{\"$search\":{\"mavar13\":\"ceci est une phrase\"}},{\"$regex\":{\"mavar14\":\"^start?aa.*\"}}]}," +
            "{\"$and\":[{\"$term\":{\"mavar14\":\"motMajuscule\",\"mavar15\":\"simplemot\"}}]}," +
            "{\"$and\":[{\"$term\":{\"mavar16\":\"motMajuscule\",\"mavar17\":\"simplemot\"}}," +
            "{\"$or\":[{\"$eq\":{\"mavar19\":\"abcd\"}},{\"$match\":{\"mavar18\":\"quelques mots\"}}]}]}," +
            "{\"$regex\":{\"mavar14\":\"^start?aa.*\"}}],\"$filter\":{\"$offset\":100,\"$limit\":1000," +
            "\"$hint\":[\"cache\"],\"$orderby\":{\"maclef1\":1,\"maclef2\":-1,\"maclef3\":1}}," +
            "\"$projection\":{\"$fields\":{\"#dua\":1,\"#all\":1},\"$usage\":\"abcdef1234\"}," +
            "\"$facets\":[{\"$name\":\"mafacet\", \"$terms\":{\"$field\":\"mavar1\",\"$size\":5,\"$order\":\"ASC\"}}," +
            "{" +
            "    \"$name\": \"filters_facet\"," +
            "    \"$filters\": {" +
            "        \"$query_filters\": [" +
            "            {\"$name\": \"StorageRules\", \"$query\": {\"$exists\": \"#management.StorageRule.Rules.Rule\"}}," +
            "            {\"$name\": \"AccessRules\",\"$query\": {\"$exists\": \"#management.AccessRule.Rules.Rule\"}}" +
            "        ]" +
            "    }" +
            "}" +
            "]}"
        );

        exampleMd = JsonHandler.getFromString(
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
            "{ $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' } } " +
            "], " +
            "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], $track_total_hits: true, " +
            "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
            "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' }," +
            "$facets : [{$name : 'mafacet', $terms : {$field : 'mavar1', $size : 1, $order: 'ASC'}}," +
            "{" +
            "    $name: 'filters_facet'," +
            "    $filters: {" +
            "        $query_filters: [" +
            "            {$name: 'StorageRules', $query: {$exists: '#management.StorageRule.Rules.Rule'}}," +
            "            {$name: 'AccessRules',$query: {$exists: '#management.AccessRule.Rules.Rule'}}" +
            "        ]\n" +
            "    }" +
            "}" +
            "] }"
        );

        nestedSearchQuery = JsonHandler.getFromString(
            "{\n" +
            "  \"$query\": [\n" +
            "    {\n" +
            "      \"$and\": [\n" +
            "        {\n" +
            "          \"$match\": {\n" +
            "            \"FileInfo.FileName\": \"Monfichier\"\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"$subobject\": {\n" +
            "            \"#qualifiers.versions\": {\n" +
            "              \"$and\": [\n" +
            "                {\n" +
            "                  \"$eq\": {\n" +
            "                    \"#qualifiers.versions.FormatIdentification.MimeType\": \"text.pdf\"\n" +
            "                  }\n" +
            "                },\n" +
            "                {\n" +
            "                  \"$lte\": {\n" +
            "                    \"version.size\": 20000\n" +
            "                  }\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"$projection\": {},\n" +
            "  \"$filters\": {},\n" +
            "\"$facets\": [\n" +
            "  {\n" +
            "    \"$name\": \"facet_testl\",\n" +
            "    \"$terms\": {\n" +
            "      \"$subobject\": \"#qualifiers.versions\",\n" +
            "      \"$field\": \"#qualifiers.versions.FormatIdentification.FormatLitteral\",\n" +
            "      \"$size\": 5,\n" +
            "      \"$order\": \"ASC\"        \n" +
            "    }\n" +
            "  }\n" +
            "]" +
            "}"
        );
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
        final int oldValue = GlobalDatasParser.limitRequest;
        try {
            GlobalDatasParser.limitRequest = 1000;
            final String longfalsecode = createLongString(GlobalDatasParser.limitRequest + 100);
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(JsonHandler.getFromString(longfalsecode));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // nothing
        } finally {
            GlobalDatasParser.limitRequest = oldValue;
        }
    }

    @Test
    public void testParseNestedQuery() {
        final SelectParserMultiple request1 = new SelectParserMultiple();
        try {
            request1.parse(nestedSearchQuery.deepCopy());
            assertNotNull(request1);
            assertTrue(
                request1
                    .getRequest()
                    .getFacets()
                    .get(0)
                    .getCurrentFacet()
                    .get("$terms")
                    .get("$subobject")
                    .asText()
                    .equals("#qualifiers.versions")
            );
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testParse() {
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(exampleBothEsMd.deepCopy());
            assertTrue("Should refuse the request since ES is not allowed", request1.hasFullTextQuery());
            request1.parse(exampleMd.deepCopy());
            assertFalse("Should accept the request since ES is not allowed", request1.hasFullTextQuery());
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(exampleBothEsMd.deepCopy());
            assertNotNull(request1);
            assertTrue("Should refuse the request since ES is not allowed", request1.hasFullTextQuery());
            final SelectMultiQuery select = new SelectMultiQuery();
            select.addRoots("id0");
            select.addQueries(path("id1", "id2"));
            select.addQueries(
                and()
                    .add(
                        exists("mavar1"),
                        missing("mavar2"),
                        isNull("mavar3"),
                        or().add(in("mavar4", 1, 2).add("maval1"), nin("mavar5", "maval2").add(true))
                    )
            );
            select.addQueries(not().add(size("mavar5", 5), gt("mavar6", 7), lte("mavar7", 8)).setExactDepthLimit(4));
            select.addQueries(
                not().add(eq("mavar8", 5), ne("mavar9", "ab"), range("mavar10", 12, true, 20, true)).setDepthLimit(10)
            );
            select.addQueries(matchPhrase("mavar11", "ceci est une phrase").setRelativeDepthLimit(0));
            select.addQueries(
                matchPhrasePrefix("mavar11", "ceci est une phrase").setMatchMaxExpansions(10).setDepthLimit(0)
            );
            select.addQueries(flt("ceci est une phrase", "mavar12", "mavar13").setRelativeDepthLimit(1));
            select.addQueries(and().add(search("mavar13", "ceci est une phrase"), regex("mavar14", "^start?aa.*")));

            select.addQueries(and().add(term("mavar14", "motMajuscule").add("mavar15", "simplemot")));
            select.addQueries(
                and()
                    .add(
                        term("mavar16", "motMajuscule").add("mavar17", "simplemot"),
                        or().add(eq("mavar19", "abcd"), match("mavar18", "quelques mots"))
                    )
            );
            select.addQueries(regex("mavar14", "^start?aa.*"));
            select.setLimitFilter(100, 1000).addHintFilter(FILTERARGS.CACHE.exactToken());
            select.addOrderByAscFilter("maclef1").addOrderByDescFilter("maclef2").addOrderByAscFilter("maclef3");
            select.addUsedProjection("#dua", "#all").setUsageProjection("abcdef1234");
            select.addFacets(terms("mafacet", "mavar1", 5, FacetOrder.ASC));
            Map<String, Query> filterQueries = new HashMap<>();
            filterQueries.put("StorageRules", exists("#management.StorageRule.Rules.Rule"));
            filterQueries.put("AccessRules", exists("#management.AccessRule.Rules.Rule"));
            select.addFacets(filters("filters_facet", filterQueries));
            final SelectParserMultiple request2 = new SelectParserMultiple();
            request2.parse(select.getFinalSelect());
            assertNotNull(request2);
            final List<Query> query1 = request1.getRequest().getQueries();
            final List<Query> query2 = request2.getRequest().getQueries();
            for (int i = 0; i < query1.size(); i++) {
                if (!query1.get(i).toString().equals(query2.get(i).toString())) {
                    System.err.println(query1.get(i));
                    System.err.println(query2.get(i));
                }
                assertTrue("TypeRequest should be equal", query1.get(i).toString().equals(query2.get(i).toString()));
            }
            assertTrue(
                "Projection should be equal",
                request1
                    .getRequest()
                    .getProjection()
                    .toString()
                    .equals(request2.getRequest().getProjection().toString())
            );
            assertTrue(
                "OrderBy should be equal",
                request1.getRequest().getFilter().toString().equals(request2.getRequest().getFilter().toString())
            );
            assertEquals(request1.getLastDepth(), request2.getLastDepth());
            assertEquals(request1.hasFullTextQuery(), request2.hasFullTextQuery());
            assertEquals(request1.getRequest().getRoots().toString(), request2.getRequest().getRoots().toString());
            assertEquals(
                request1.getRequest().getFinalSelect().toString(),
                request2.getRequest().getFinalSelect().toString()
            );
            assertTrue("Command should be equal", request1.toString().equals(request2.toString()));
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFilterParse() {
        final SelectParserMultiple request = new SelectParserMultiple();
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            // empty
            request.filterParse(select.getFilter());
            assertNull("Hint should be null", request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            assertNotNull(
                "Limit should not be null",
                request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken())
            );
            assertNull("Offset should be null", request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken()));
            assertNull(
                "OrderBy should be null",
                request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken())
            );
            // hint set
            select.addHintFilter(FILTERARGS.CACHE.exactToken());
            request.filterParse(select.getFilter());
            assertEquals(
                "Hint should be True",
                FILTERARGS.CACHE.exactToken(),
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()).get(0).asText()
            );
            // hint reset
            select.resetHintFilter();
            request.filterParse(select.getFilter());
            assertNull("Hint should be null", request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            // limit set
            select.setLimitFilter(0, 1000);
            request.filterParse(select.getFilter());
            assertEquals(1000, request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken()).asLong());
            assertNull("Offset should be null", request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken()));
            // offset set
            select.setLimitFilter(100, 0);
            request.filterParse(select.getFilter());
            assertEquals(100, request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken()).asLong());
            assertEquals(
                GlobalDatas.LIMIT_LOAD,
                request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken()).asLong()
            );
            // orderBy set through array
            select.addOrderByAscFilter("var1", "var2").addOrderByDescFilter("var3");
            request.filterParse(select.getFilter());
            assertNotNull(
                "OrderBy should not be null",
                request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken())
            );
            // check both
            assertEquals(3, request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
            for (
                final Iterator<Entry<String, JsonNode>> iterator = request
                    .getRequest()
                    .getFilter()
                    .get(SELECTFILTER.ORDERBY.exactToken())
                    .fields();
                iterator.hasNext();
            ) {
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
            assertNull(
                "OrderBy should be null",
                request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken())
            );
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testProjectionParse() {
        final SelectParserMultiple request = new SelectParserMultiple();
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            // empty
            request.filterParse(select.getFilter());
            assertNull("Hint should be null", request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            assertNotNull(
                "Limit should not be null",
                request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken())
            );
            assertNull("Offset should be null", request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken()));
            assertNull(
                "OrderBy should be null",
                request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken())
            );
            // hint set
            select.addHintFilter(FILTERARGS.CACHE.exactToken());
            request.filterParse(select.getFilter());
            assertEquals(
                "Hint should be True",
                FILTERARGS.CACHE.exactToken(),
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()).get(0).asText()
            );
            // hint reset
            select.resetHintFilter();
            request.filterParse(select.getFilter());
            assertNull("Hint should be null", request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            // limit set
            select.setLimitFilter(0, 1000);
            request.filterParse(select.getFilter());
            assertEquals(1000, request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken()).asLong());
            assertNull("Offset should be null", request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken()));
            // offset set
            select.setLimitFilter(100, 0);
            request.filterParse(select.getFilter());
            assertEquals(100, request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken()).asLong());
            assertEquals(
                GlobalDatas.LIMIT_LOAD,
                request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken()).asLong()
            );
            // orderBy set through array
            select.addOrderByAscFilter("var1", "var2").addOrderByDescFilter("var3");
            request.filterParse(select.getFilter());
            assertNotNull(
                "OrderBy should not be null",
                request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken())
            );
            // check both
            assertEquals(3, request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
            for (
                final Iterator<Entry<String, JsonNode>> iterator = request
                    .getRequest()
                    .getFilter()
                    .get(SELECTFILTER.ORDERBY.exactToken())
                    .fields();
                iterator.hasNext();
            ) {
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
            assertNull(
                "OrderBy should be null",
                request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken())
            );
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFacetsParse() {
        ArrayNode facetsArray = JsonHandler.createArrayNode();
        final SelectParserMultiple request = new SelectParserMultiple();
        try {
            // empty rootNode
            request.facetsParse(facetsArray);
            assertEquals("Facets should be empty", 0, request.getRequest().getFacets().size());

            // one facet
            facetsArray.add(terms("mafacet", "mavar1", 1, FacetOrder.ASC).getCurrentFacet());
            request.facetsParse(facetsArray);
            assertEquals("Facets should have 1 facet", 1, request.getRequest().getFacets().size());

            // multiple facets
            facetsArray.add(terms("mafacet2", "mavar1", 5, FacetOrder.DESC).getCurrentFacet());
            request.facetsParse(facetsArray);
            assertEquals("Facets should have 2 facet", 2, request.getRequest().getFacets().size());

            // null facets
            assertThatCode(() -> {
                final ArrayNode arrayNode = null;
                request.facetsParse(arrayNode);
            }).doesNotThrowAnyException();

            // fake facet, empty node
            assertThatThrownBy(() -> {
                ObjectNode fakeFacet = JsonHandler.createObjectNode();
                facetsArray.removeAll();
                facetsArray.add(fakeFacet);
                request.facetsParse(facetsArray);
            }).isInstanceOf(InvalidParseOperationException.class);

            // fake facet, name instead of $neame
            assertThatThrownBy(() -> {
                ObjectNode fakeFacet = JsonHandler.createObjectNode();
                fakeFacet.put("name", "mafacet");
                facetsArray.removeAll();
                facetsArray.add(fakeFacet);
                request.facetsParse(facetsArray);
            }).isInstanceOf(InvalidParseOperationException.class);

            // fake facet, only $name, invalid facet command
            assertThatThrownBy(() -> {
                ObjectNode fakeFacet = JsonHandler.createObjectNode();
                fakeFacet.put("$name", "mafacet");
                fakeFacet.put("$wrong", "wrong");
                facetsArray.removeAll();
                facetsArray.add(fakeFacet);
                request.facetsParse(facetsArray);
            }).isInstanceOf(InvalidParseOperationException.class);

            // fake facet, only $name, invalid $terms value
            assertThatThrownBy(() -> {
                ObjectNode fakeFacet = JsonHandler.createObjectNode();
                fakeFacet.put("$name", "mafacet");
                fakeFacet.put("$terms", "not valid");
                facetsArray.removeAll();
                facetsArray.add(fakeFacet);
                request.facetsParse(facetsArray);
            }).isInstanceOf(InvalidParseOperationException.class);

            // fake facet, only $name, invalid $filters value
            assertThatThrownBy(() -> {
                ObjectNode fakeFacet = JsonHandler.createObjectNode();
                fakeFacet.put("$name", "mafacet");
                ObjectNode fakeQueryFilter = JsonHandler.createObjectNode();
                fakeQueryFilter.set("$query_filters", JsonHandler.createObjectNode());
                fakeFacet.set("$filters", fakeQueryFilter);
                facetsArray.removeAll();
                facetsArray.add(fakeFacet);
                request.facetsParse(facetsArray);
            }).isInstanceOf(InvalidParseOperationException.class);
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testSelectParser() throws InvalidParseOperationException {
        final SelectParserMultiple request = new SelectParserMultiple();
        request.parse(exampleMd.deepCopy());
        assertNotNull(request);

        final SelectParserMultiple request2 = new SelectParserMultiple(new VarNameAdapter());
        assertNotNull(request2);
    }

    @Test
    public void testParseQueryOnly() throws InvalidParseOperationException {
        final SelectParserMultiple request = new SelectParserMultiple();
        final String ex = "{}";
        request.parseQueryOnly(ex);
        assertNotNull(request);
    }

    @Test
    public void testParseQueryWithTrackTotalHits() throws InvalidParseOperationException {
        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        selectMultiQuery.trackTotalHits(true);

        final SelectParserMultiple request = new SelectParserMultiple();
        request.parse(selectMultiQuery.getFinalSelect());

        assertThat(request.trackTotalHits()).isTrue();
    }

    @Test
    public void testParseQueryWithoutTrackTotalHits() throws InvalidParseOperationException {
        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

        final SelectParserMultiple request = new SelectParserMultiple();
        request.parse(selectMultiQuery.getFinalSelect());

        assertThat(request.trackTotalHits()).isFalse();
    }

    @Test
    public void testAddConditionParseSelect() throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserMultiple request = new SelectParserMultiple();
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(and().add(term("var01", "value1"), gte("var02", 3)).setDepthLimit(10));
        select.addQueries(and().add(term("var11", "value2"), gte("var12", 4)));
        select.addQueries(and().add(term("var13", "value2"), gte("var14", 4)).setExactDepthLimit(12));
        select
            .addOrderByAscFilter("var1")
            .addOrderByDescFilter("var2")
            .addUsedProjection("var3")
            .addUnusedProjection("var4");
        select.addRoots("id1", "id2");
        select.addHintFilter(FILTERARGS.UNITS.exactToken(), FILTERARGS.NOCACHE.exactToken());
        select.addFacets(FacetHelper.terms("myFacet", "#id", 5, FacetOrder.ASC));
        select.addFacets(FacetHelper.terms("myFacet2", "Name", 1, FacetOrder.DESC));
        request.parse(select.getFinalSelect());
        assertNotNull(request.getRequest());

        assertEquals(
            "{\"$roots\":[\"id2\",\"id1\"],\"$query\":[{\"$and\":[{\"$term\":{\"var01\":\"value1\"}}," +
            "{\"$gte\":{\"var02\":3}}]},{\"$and\":[{\"$term\":{\"var11\":\"value2\"}},{\"$gte\":{\"var12\":4}}]}," +
            "{\"$and\":[{\"$term\":{\"var13\":\"value2\"}},{\"$gte\":{\"var14\":4}}]}]," +
            "\"$filter\":{\"$orderby\":{\"var1\":1,\"var2\":-1},\"$hint\":[\"units\",\"nocache\"]}," +
            "\"$projection\":{\"$fields\":{\"var3\":1,\"var4\":0}},\"$facets\":[{\"$name\":\"myFacet\"," +
            "\"$terms\":{\"$field\":\"#id\",\"$size\":5,\"$order\":\"ASC\"}},{\"$name\":\"myFacet2\"," +
            "\"$terms\":{\"$field\":\"Name\",\"$size\":1,\"$order\":\"DESC\"}}]}",
            request.getRootNode().toString()
        );
    }

    @Test
    public void testInternalParseSelect() throws InvalidParseOperationException {
        final SelectParserMultiple request = new SelectParserMultiple();
        final String s = "[ [ 'id0' ], { $path : [ 'id1', 'id2'] }, {$mult : false }, {} ]";
        request.parse(JsonHandler.getFromString(s));
        assertNotNull(request);
    }

    @Test
    public void testWrongParseSelect() throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserMultiple request = new SelectParserMultiple();
        String s =
            "{\"$roots\":[]," +
            "\"$query\":[{\"$and\":[{\"$and\":[{\"$term\":{\"_id\":\"value1\"}},{\"$gte\":{\"var02\":3}}]}," +
            "{\"$eq\":{\"var5\":\"value\"}}]}," +
            "{\"$and\":[{\"$term\":{\"var11\":\"value2\"}},{\"$gte\":{\"var12\":4}}]}]," +
            "\"$filter\":{\"$limit\":10000,\"$orderby\":{\"var1\":1,\"var2\":-1}}," +
            "\"$projection\":{\"$fields\":{\"var3\":1,\"var4\":0}}}";
        try {
            request.parse(JsonHandler.getFromString(s));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // OK
        }
        s = "{\"$roots\":[]," +
        "\"$query\":[{\"$and\":[{\"$and\":[{\"$term\":{\"var01\":\"value1\"}},{\"$gte\":{\"var02\":3}}]}," +
        "{\"$eq\":{\"var5\":\"value\"}}]}," +
        "{\"$and\":[{\"$term\":{\"var11\":\"value2\"}},{\"$gte\":{\"var12\":4}}]}]," +
        "\"$filter\":{\"$limit\":10000,\"$orderby\":{\"var1\":1,\"var2\":-1}}," +
        "\"$projection\":{\"$fields\":{\"_id\":1,\"var4\":0}}}";
        try {
            request.parse(JsonHandler.getFromString(s));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // OK
        }
        s = "{\"$roots\":[]," +
        "\"$query\":[{\"$and\":[{\"$and\":[{\"$term\":{\"var01\":\"value1\"}},{\"$gte\":{\"var02\":3}}]}," +
        "{\"$eq\":{\"var5\":\"value\"}}]}," +
        "{\"$and\":[{\"$term\":{\"var11\":\"value2\"}},{\"$gte\":{\"var12\":4}}]}]," +
        "\"$filter\":{\"$limit\":10000,\"$orderby\":{\"_id\":1,\"var2\":-1}}," +
        "\"$projection\":{\"$fields\":{\"var3\":1,\"var4\":0}}}";
        try {
            request.parse(JsonHandler.getFromString(s));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // OK
        }

        s = "{\"$roots\":[]," +
        "\"$query\":[{\"$eq\":{\"var5\":\"value\"}}]," +
        "\"$filter\":{\"$limit\":10000}," +
        "\"$projection\":{\"$fields\":{\"var3\":1,\"var4\":0}}," +
        "\"$facets\":[{\"$name\":\"myFacet\"]}";
        try {
            request.parse(JsonHandler.getFromString(s));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // OK
        }

        s = "{\"$roots\":[]," +
        "\"$query\":[{\"$eq\":{\"var5\":\"value\"}}]," +
        "\"$filter\":{\"$limit\":10000}," +
        "\"$projection\":{\"$fields\":{\"var3\":1,\"var4\":0}}," +
        "\"$facets\":[{\"$name\":\"mafacet\", \"$terms\":{\"$field\":\"mavar1\"}}]}";
        try {
            request.parse(JsonHandler.getFromString(s));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // OK
        }

        s = "{\"$roots\":[]," +
        "\"$query\":[{\"$eq\":{\"var5\":\"value\"}}]," +
        "\"$filter\":{\"$limit\":10000}," +
        "\"$projection\":{\"$fields\":{\"var3\":1,\"var4\":0}}," +
        "\"$facets\":[{\"$name\":\"mafacet\", \"$terms\":{\"$field\":\"mavar1\",\"$order\":\"ASC\"}}]}";
        try {
            request.parse(JsonHandler.getFromString(s));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // OK
        }

        s = "{\"$roots\":[]," +
        "\"$query\":[{\"$eq\":{\"var5\":\"value\"}}]," +
        "\"$filter\":{\"$limit\":10000}," +
        "\"$projection\":{\"$fields\":{\"var3\":1,\"var4\":0}}," +
        "\"$facets\":[{\"$name\":\"mafacet\", \"$terms\":{\"$field\":\"mavar1\",\"$size\":1}}]}";
        try {
            request.parse(JsonHandler.getFromString(s));
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // OK
        }
    }
}
