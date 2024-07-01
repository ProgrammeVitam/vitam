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
package fr.gouv.vitam.common.database.translators.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QueryToElasticsearchTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(QueryToElasticsearchTest.class);

    private static final String exampleElasticsearch =
        "{ $roots : [ 'id0' ], $query : [ " +
        "{ $and : [ " +
        "{$exists : 'mavar1'}, " +
        "{$missing : 'mavar2'}, " +
        "{$isNull : 'mavar3'}, " +
        "{ $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, " +
        "{ $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ " +
        "{ $size : { 'mavar5' : 5 } }, " +
        "{ $gt : { 'mavar6' : 7 } }, " +
        "{ $lt : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
        "{ $not : [ " +
        "{ $eq : { 'mavar8' : 5 } }, " +
        "{ $ne : { 'mavar9' : 'ab' } }, " +
        "{ $wildcard : { 'mavar9' : 'ab' } }, " +
        "{ $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 }, " +
        "{ $range : { 'mavar16' : { $gt : 13, $lt : 29} } }," +
        "{ $gte : { 'mavar17' : 100 } }," +
        "{ $lte : { 'mavar18' : 56 } }," +
        "{ $match : { 'mavar19' : 'words' , '$max_expansions' : 1  } }," +
        "{ $match_phrase : { 'mavar20' : 'words', '$max_expansions' : 1 } }," +
        "{ $match_phrase_prefix : { 'mavar21' : 'phrase', '$max_expansions' : 1 } }," +
        "{ $match : { 'mavar19' : 'words' } }," +
        "{ $match_phrase : { 'mavar20' : 'words'} }," +
        "{ $match_phrase_prefix : { 'mavar21' : 'phrase'} }," +
        "{ $search : { 'mavar25' : 'searchParameter' } }" +
        "], " +
        "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
        "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
        "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' }, " +
        "$facets: [{$name : 'mafacet', $terms : {$field : 'mavar1', $size : 1, $order: 'ASC'} }," +
        "{" +
        "    $name: 'filters_facet'," +
        "    $filters: {" +
        "        $query_filters: [" +
        "            {$name: 'StorageRules', $query: {$exists: '#management.StorageRule.Rules.Rule'}}," +
        "            {$name: 'AccessRules',$query: {$exists: '#management.AccessRule.Rules.Rule'}}" +
        "        ]" +
        "    }" +
        "}" +
        "] }";

    private static JsonNode example;

    private static JsonNode nestedSearchQuery;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        example = JsonHandler.getFromString(exampleElasticsearch);
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

    private SelectParserMultiple createSelect(JsonNode query) {
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(query);
            assertNotNull(request1);
            return request1;
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void testGetCommands() {
        try {
            final SelectParserMultiple parser = createSelect(example);
            final SelectMultiQuery select = parser.getRequest();
            final QueryBuilder queryBuilderRoot = QueryToElasticsearch.getRoots("_up", select.getRoots());
            DynamicParserTokens parserTokens = new DynamicParserTokens(
                new VitamDescriptionResolver(Collections.emptyList()),
                Collections.emptyList()
            );
            final List<SortBuilder<?>> sortBuilders = QueryToElasticsearch.getSorts(parser, true, parserTokens);
            final List<AggregationBuilder> facetBuilders = QueryToElasticsearch.getFacets(parser, parserTokens);
            assertEquals(4, sortBuilders.size());
            assertEquals(2, facetBuilders.size());

            final List<Query> list = select.getQueries();
            for (int i = 0; i < list.size(); i++) {
                System.out.println(i + " = " + list.get(i).toString());
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(
                    list.get(i),
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = QueryToElasticsearch.getFullCommand(
                    queryBuilderCommand,
                    queryBuilderRoot
                );
                System.out.println(i + " = " + ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest));
            }
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void checkNoUidAfterUpgradeToElastic7() {
        try {
            final SelectParserMultiple parser;
            try {
                parser = new SelectParserMultiple(new FakeMetadataVarNameAdapter());
                parser.parse(
                    JsonHandler.getFromString(
                        "{ $roots : [ 'id0' ], $query : [ " +
                        "{ $exists : '#id'}, " +
                        "{ $missing : '#id'}, " +
                        "{ $isNull : '#id'}, " +
                        "{ $lt : { '#id' : '8' } }, " +
                        "{ $eq : { '#id' : 'ab' } }, " +
                        "{ $and : [ { $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] } ] }, " +
                        "{ $or : [ { $or : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] } ] }, " +
                        "{ $and : [ { $or : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] } ] }, " +
                        "{ $or : [ { $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] } ] } " +
                        "], " +
                        "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
                        "$orderby : { #id : 1, maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
                        "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' }, " +
                        "$facets: [" +
                        "{$name : 'mafacet', $terms : {$field : '#id', $size : 1, $order: 'ASC'} }," +
                        "{$name : 'EndDate', $date_range : { $field : 'EndDate',$format : 'yyyy',$ranges: [{$from: '1800',$to: '2080'}]} }," +
                        "{$name : 'EndDate2', $date_range : { $field : 'EndDate',$format : 'yyyy',$ranges: [{$from: '1800'}]} }] }," +
                        "{$name : 'facetFilters', $filters : { $field : 'EndDate',$format : 'yyyy',$ranges: [{$from: '1800'}]} }] }," +
                        "{" +
                        "    $name: 'filters_facet'," +
                        "    $filters: {" +
                        "        $query_filters: [" +
                        "            {$name: 'StorageRules', $query: {$exists: '#management.StorageRule.Rules.Rule'}}," +
                        "            {$name: 'AccessRules',$query: {$exists: '#management.AccessRule.Rules.Rule'}}" +
                        "        ]" +
                        "    }" +
                        "}" +
                        "] }"
                    )
                );
            } catch (final Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
                return;
            }

            final SelectMultiQuery select = parser.getRequest();
            final QueryBuilder queryBuilderRoot = QueryToElasticsearch.getRoots("_up", select.getRoots());
            DynamicParserTokens parserTokens = new DynamicParserTokens(
                new VitamDescriptionResolver(Collections.emptyList()),
                Collections.emptyList()
            );
            final List<SortBuilder<?>> sortBuilders = QueryToElasticsearch.getSorts(parser, true, parserTokens);
            final List<AggregationBuilder> facetBuilders = QueryToElasticsearch.getFacets(parser, parserTokens);
            assertEquals(4, sortBuilders.size());
            assertEquals(3, facetBuilders.size());

            final List<Query> list = select.getQueries();
            // exists #id
            {
                int i = 0;
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(
                    list.get(i),
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = QueryToElasticsearch.getFullCommand(
                    queryBuilderCommand,
                    queryBuilderRoot
                );
                assertFalse(ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest).contains("_uid"));
            }
            // missing #id
            {
                int i = 1;
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(
                    list.get(i),
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = QueryToElasticsearch.getFullCommand(
                    queryBuilderCommand,
                    queryBuilderRoot
                );
                assertFalse(ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest).contains("_uid"));
            }
            // isNull #id
            {
                int i = 2;
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(
                    list.get(i),
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = QueryToElasticsearch.getFullCommand(
                    queryBuilderCommand,
                    queryBuilderRoot
                );
                assertFalse(ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest).contains("_uid"));
            }
            // lt #id
            {
                int i = 3;
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(
                    list.get(i),
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = QueryToElasticsearch.getFullCommand(
                    queryBuilderCommand,
                    queryBuilderRoot
                );
                assertTrue(ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest).contains("_id"));
                assertFalse(
                    ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest).contains("typeunique#8")
                );
            }
            // eq #id
            {
                int i = 4;
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(
                    list.get(i),
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = QueryToElasticsearch.getFullCommand(
                    queryBuilderCommand,
                    queryBuilderRoot
                );
                assertFalse(ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest).contains("_uid"));
            }
            // and and
            {
                int i = 5;
                assertEquals(
                    list.get(i).getCurrentQuery().toString().indexOf("$and"),
                    list.get(i).getCurrentQuery().toString().lastIndexOf("$and")
                );
            }
            // or or
            {
                int i = 6;
                assertEquals(
                    list.get(i).getCurrentQuery().toString().indexOf("$or"),
                    list.get(i).getCurrentQuery().toString().lastIndexOf("$or")
                );
            }
            // and or
            {
                int i = 7;
                assertTrue(
                    list.get(i).getCurrentQuery().toString().contains("$and") &&
                    list.get(i).getCurrentQuery().toString().contains("$or")
                );
            }
            // or and
            {
                int i = 8;
                assertTrue(
                    list.get(i).getCurrentQuery().toString().contains("$and") &&
                    list.get(i).getCurrentQuery().toString().contains("$or")
                );
            }
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetNestedSearchCommand() {
        try {
            final SelectParserMultiple parser = createSelect(nestedSearchQuery);
            final SelectMultiQuery select = parser.getRequest();
            assertEquals(
                "#qualifiers.versions",
                select.getFacets().get(0).getCurrentFacet().get("$terms").get("$subobject").asText()
            );
            DynamicParserTokens parserTokens = new DynamicParserTokens(
                new VitamDescriptionResolver(Collections.emptyList()),
                Collections.emptyList()
            );
            final List<AggregationBuilder> facetBuilders = QueryToElasticsearch.getFacets(parser, parserTokens);
            assertEquals(1, facetBuilders.size());
            assertTrue(facetBuilders.get(0) instanceof NestedAggregationBuilder);
            final QueryBuilder queryBuilderRoot = QueryToElasticsearch.getRoots("_up", select.getRoots());
            final List<SortBuilder<?>> sortBuilders = QueryToElasticsearch.getSorts(parser, true, parserTokens);
            assertEquals(1, sortBuilders.size());

            final List<Query> list = select.getQueries();
            for (int i = 0; i < list.size(); i++) {
                System.out.println(i + " = " + list.get(i).toString());
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(
                    list.get(i),
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = QueryToElasticsearch.getFullCommand(
                    queryBuilderCommand,
                    queryBuilderRoot
                );
                System.out.println(i + " = " + ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest));
            }

            {
                final List<Query> queries = select.getQueries();
                assertEquals(1, queries.size());
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(
                    list.get(0),
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = QueryToElasticsearch.getFullCommand(
                    queryBuilderCommand,
                    queryBuilderRoot
                );
                assertTrue(ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest).contains("nested"));
            }
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
