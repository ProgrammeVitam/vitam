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
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import fr.gouv.vitam.common.database.parser.request.multiple.DeleteParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RequestToElasticsearchTest {

    private static JsonNode exampleSelectElasticsearch;
    private static JsonNode nestedSearchQuery;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        exampleSelectElasticsearch = JsonHandler.getFromString(
            "{ $roots : [ 'id0' ], $query : [ " +
            "{ $and : [ " +
            "{$exists : 'mavar1'}, " +
            "{$missing : 'mavar2'}, " +
            "{$isNull : 'mavar3'}, " +
            "{ $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, " +
            "{ $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
            "{ $not : [ " +
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
            "{ $match_phrase : { 'mavar20' : 'words' } }," +
            "{ $match_phrase_prefix : { 'mavar21' : 'phrase' } }," +
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
            "  \"$filters\": {}\n" +
            "}"
        );
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Test
    public void testGetRequestToElasticsearch() {
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(exampleSelectElasticsearch);
            assertNotNull(request1);
            assertTrue(RequestToElasticsearch.getRequestToElasticsearch(request1) instanceof SelectToElasticsearch);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetCommands() {
        try {
            final SelectToElasticsearch rte = createSelect(exampleSelectElasticsearch);
            final QueryBuilder queryBuilderRoot = rte.getInitialRoots("_up");
            final int size = rte.getNbQueries();
            DynamicParserTokens parserTokens = new DynamicParserTokens(
                new VitamDescriptionResolver(Collections.emptyList()),
                Collections.emptyList()
            );
            for (int i = 0; i < size; i++) {
                final QueryBuilder queryBuilderCommand = rte.getNthQueries(
                    i,
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = rte.getRequest(queryBuilderCommand, queryBuilderRoot);
                System.out.println(i + " = " + ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest));
            }
            try {
                rte.getNthQueries(size, new FakeMetadataVarNameAdapter(), parserTokens);
                fail("Should failed");
            } catch (final IllegalAccessError e) {}
            assertNotNull(rte.getRequest());
            assertNotNull(rte.getNthQuery(0));
            assertNotNull(rte.getRequestParser());
            assertNotNull(rte.model());
            System.out.println(
                "Select Context = " +
                rte.getLastDepth() +
                ":" +
                rte.getFinalLimit() +
                ":" +
                rte.getFinalOffset() +
                ":" +
                rte.getUsage() +
                ":" +
                rte.getHints()
            );
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test(expected = VitamException.class)
    public void shouldRaiseException_whenRequestIsNotAllowed() throws InvalidCreateOperationException, VitamException {
        final DeleteParserMultiple request1 = new DeleteParserMultiple();
        RequestToElasticsearch.getRequestToElasticsearch(request1);
    }

    @Test
    public void testGetNestedRequestToElasticsearch() {
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(nestedSearchQuery);
            assertNotNull(request1);
            assertTrue(RequestToElasticsearch.getRequestToElasticsearch(request1) instanceof SelectToElasticsearch);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetNestedCommands() {
        try {
            final SelectToElasticsearch rte = createSelect(nestedSearchQuery);
            final QueryBuilder queryBuilderRoot = rte.getInitialRoots("_up");
            final int size = rte.getNbQueries();
            DynamicParserTokens parserTokens = new DynamicParserTokens(
                new VitamDescriptionResolver(Collections.emptyList()),
                Collections.emptyList()
            );
            for (int i = 0; i < size; i++) {
                final QueryBuilder queryBuilderCommand = rte.getNthQueries(
                    i,
                    new FakeMetadataVarNameAdapter(),
                    parserTokens
                );
                final QueryBuilder queryBuilderseudoRequest = rte.getRequest(queryBuilderCommand, queryBuilderRoot);
                System.out.println(i + " = " + ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest));
            }
            try {
                rte.getNthQueries(size, new FakeMetadataVarNameAdapter(), parserTokens);
                fail("Should failed");
            } catch (final IllegalAccessError e) {}
            assertNotNull(rte.getRequest());
            assertNotNull(rte.getNthQuery(0));
            assertNotNull(rte.getRequestParser());
            assertNotNull(rte.model());
            System.out.println(
                "Select Context = " +
                rte.getLastDepth() +
                ":" +
                rte.getFinalLimit() +
                ":" +
                rte.getFinalOffset() +
                ":" +
                rte.getUsage() +
                ":" +
                rte.getHints()
            );
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private SelectToElasticsearch createSelect(JsonNode query) {
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(query);
            assertNotNull(request1);
            return new SelectToElasticsearch(request1);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }
}
