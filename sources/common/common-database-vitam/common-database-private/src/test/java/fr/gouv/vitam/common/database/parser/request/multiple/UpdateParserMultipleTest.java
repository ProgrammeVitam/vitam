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
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.MULTIFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.flt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gt;
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
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.add;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.inc;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.max;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.min;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.pop;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.pull;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.push;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.rename;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.set;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.unset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class UpdateParserMultipleTest {

    private static JsonNode updateAction;

    private static JsonNode exampleBothEsMd;

    private static JsonNode exampleMd;

    private static JsonNode nestedSearchQuery;

    @BeforeClass
    public static void init() throws InvalidParseOperationException {
        VitamLoggerFactory.setLogLevel(VitamLogLevel.INFO);
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

        exampleBothEsMd = JsonHandler.getFromString(
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
            "{ $eq : { 'mavar8' : 5 } }, { " +
            "$ne : { 'mavar9' : 'ab' } }, { " +
            "$range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}," +
            "{ $match_phrase : { 'mavar11' : 'ceci est une phrase' }, $depth : 0}," +
            "{ $match_phrase_prefix : { 'mavar11' : 'ceci est une phrase', $max_expansions : 10 }, $depth : 0}," +
            "{ $flt : { $fields : [ 'mavar12', 'mavar13' ], $like : 'ceci est une phrase' }, $depth : 1}," +
            "{ $and : [ " +
            "{ $search : { 'mavar13' : 'ceci est une phrase' } }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' } } ] }," +
            "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
            "{ $and : [ " +
            "{ $term : { 'mavar16' : 'motMajuscule', 'mavar17' : 'simplemot' } }, " +
            "{ $or : [ {$eq : { 'mavar19' : 'abcd' } }, { $match : { 'mavar18' : 'quelques mots' } } ] } ] }, " +
            "{ $regex : { 'mavar14' : '^start?aa.*' } } " +
            "], " +
            "$filter : {$mult : false }," +
            "$action : " +
            updateAction +
            " }"
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
            "$filter : {$mult : false }," +
            "$action : " +
            updateAction +
            " }"
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
            "  \"$filters\": {}\n," +
            " \"$action\" : " +
            updateAction +
            " }"
        );
    }

    @Test
    public void testParse() {
        try {
            final UpdateParserMultiple request1 = new UpdateParserMultiple();
            request1.parse(exampleBothEsMd.deepCopy());
            assertTrue("Should refuse the request since ES is not allowed", request1.hasFullTextQuery());
            request1.parse(exampleMd.deepCopy());
            assertFalse("Should accept the request since ES is not allowed", request1.hasFullTextQuery());
        } catch (final Exception e) {}
        try {
            final UpdateParserMultiple request1 = new UpdateParserMultiple();
            request1.parse(exampleBothEsMd.deepCopy());
            assertNotNull(request1);
            assertTrue("Should refuse the request since ES is not allowed", request1.hasFullTextQuery());
            assertNotEquals("Data should not be empty", 0, request1.getRequest().getActions().size());
            final UpdateMultiQuery update = new UpdateMultiQuery();
            update.addRoots("id0");
            update.addQueries(path("id1", "id2"));
            update.addQueries(
                and()
                    .add(
                        exists("mavar1"),
                        missing("mavar2"),
                        isNull("mavar3"),
                        or().add(in("mavar4", 1, 2).add("maval1"), nin("mavar5", "maval2").add(true))
                    )
            );
            update.addQueries(not().add(size("mavar5", 5), gt("mavar6", 7), lte("mavar7", 8)).setExactDepthLimit(4));
            update.addQueries(
                not().add(eq("mavar8", 5), ne("mavar9", "ab"), range("mavar10", 12, true, 20, true)).setDepthLimit(1)
            );
            update.addQueries(matchPhrase("mavar11", "ceci est une phrase").setRelativeDepthLimit(0));
            update.addQueries(
                matchPhrasePrefix("mavar11", "ceci est une phrase").setMatchMaxExpansions(10).setDepthLimit(0)
            );
            update.addQueries(flt("ceci est une phrase", "mavar12", "mavar13").setRelativeDepthLimit(1));
            update.addQueries(and().add(search("mavar13", "ceci est une phrase"), regex("mavar14", "^start?aa.*")));
            update.addQueries(and().add(term("mavar14", "motMajuscule").add("mavar15", "simplemot")));
            update.addQueries(
                and()
                    .add(
                        term("mavar16", "motMajuscule").add("mavar17", "simplemot"),
                        or().add(eq("mavar19", "abcd"), match("mavar18", "quelques mots"))
                    )
            );
            update.addQueries(regex("mavar14", "^start?aa.*"));

            update.setMult(false);

            update.addActions(set("mavar1", 1).add("mavar2", 1.2).add("mavar3", true).add("mavar4", "ma chaine"));
            update.addActions(unset("mavar5", "mavar6"));
            update.addActions(inc("mavar7", 2), min("mavar8", 3), min("mavar16", 12), max("mavar9", 3));
            update.addActions(rename("mavar10", "mavar11"));
            update.addActions(push("mavar12", 1, 2), add("mavar13", 1, 2));
            update.addActions(pop("mavar14", -1), pull("mavar15", 1, 2));

            final UpdateParserMultiple request2 = new UpdateParserMultiple();
            request2.parse(exampleBothEsMd.deepCopy());
            assertNotNull(request2);
            final List<Query> query1 = request1.getRequest().getQueries();
            final List<Query> query2 = request2.getRequest().getQueries();
            for (int i = 0; i < query1.size(); i++) {
                assertEquals("TypeRequest should be equal", query1.get(i).toString(), query2.get(i).toString());
            }
            final List<Action> actions1 = request1.getRequest().getActions();
            final List<Action> actions2 = request2.getRequest().getActions();
            assertEquals("Action should be equal", actions1.size(), actions2.size());
            for (int i = 0; i < actions1.size(); i++) {
                if (!actions1.get(i).toString().equals(actions2.get(i).toString())) {
                    System.out.println(actions1.get(i).toString() + " != " + actions2.get(i).toString());
                }
                assertTrue(
                    "Action should be equal: " + i,
                    actions1.get(i).toString().equals(actions2.get(i).toString())
                );
            }
            assertTrue(
                "OrderBy should be equal",
                request1.getRequest().getFilter().toString().equals(request2.getRequest().getFilter().toString())
            );
            assertEquals(request1.getLastDepth(), request2.getLastDepth());
            assertEquals(request1.hasFullTextQuery(), request2.hasFullTextQuery());
            assertEquals(request1.getRequest().getRoots().toString(), request2.getRequest().getRoots().toString());
            assertEquals(
                request1.getRequest().getFinalUpdate().toString(),
                request2.getRequest().getFinalUpdate().toString()
            );
            assertTrue("Command should be equal", request1.toString().equals(request2.toString()));
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFilterParse() {
        final UpdateParserMultiple request = new UpdateParserMultiple();
        final UpdateMultiQuery update = new UpdateMultiQuery();
        try {
            // empty
            request.filterParse(update.getFilter());
            assertNull("Hint should be null", request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            assertNull("Limit should be null", request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken()));
            assertNull("Offset should be null", request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken()));
            assertNull(
                "OrderBy should be null",
                request.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken())
            );
            assertNull("Mult should be null", request.getRequest().getFilter().get(MULTIFILTER.MULT.exactToken()));
            // hint set
            update.addHintFilter(FILTERARGS.CACHE.exactToken());
            request.filterParse(update.getFilter());
            assertEquals(
                "Hint should be True",
                FILTERARGS.CACHE.exactToken(),
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()).get(0).asText()
            );
            // hint reset
            update.resetHintFilter();
            request.filterParse(update.getFilter());
            assertNull("Hint should be null", request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            // multi set
            update.setMult(false);
            request.filterParse(update.getFilter());
            assertEquals(false, request.getRequest().getFilter().get(MULTIFILTER.MULT.exactToken()).asBoolean());
            update.setMult(true);
            request.filterParse(update.getFilter());
            assertEquals(true, request.getRequest().getFilter().get(MULTIFILTER.MULT.exactToken()).asBoolean());
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testActionParse() {
        final UpdateParserMultiple request = new UpdateParserMultiple();
        final UpdateMultiQuery update = new UpdateMultiQuery();
        try {
            // empty rootNode
            request.actionParse(JsonHandler.createArrayNode());
            assertEquals("Data should be empty", 0, request.getRequest().getActions().size());
            request.actionParse(updateAction);
            assertNotEquals("Data should not be empty", 0, request.getRequest().getActions().size());
            final List<Action> actions = request.getRequest().getActions();
            for (final Action action : actions) {
                update.addActions(action);
            }
            assertEquals(request.getRequest().getFinalUpdate().toString(), update.getFinalUpdate().toString());
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        JsonNode node;
        try {
            node = JsonHandler.getFromString("[ { $set : { var : '#id' } } ]");
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
            return;
        }
        try {
            request.actionParse(node);
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            node = JsonHandler.getFromString("[ { $set : { #id : 1 } } ]");
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
            return;
        }
        try {
            request.actionParse(node);
            fail("Should Failed");
        } catch (final InvalidParseOperationException e) {}
    }

    @Test
    public void testUpdateParser() throws InvalidParseOperationException {
        final UpdateParserMultiple request = new UpdateParserMultiple();
        request.parse(exampleMd.deepCopy());
        assertNotNull(request);

        final UpdateParserMultiple request2 = new UpdateParserMultiple(new VarNameAdapter());
        assertNotNull(request2);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void testExternalUnautorized() throws InvalidParseOperationException {
        String queryString =
            "{\"$query\": [],\"$filter\": {},\"$action\": [\n" +
            "\t\t{\"$set\": {\n" +
            "\t\t\t\t\"_og\" : \"\"\n" +
            "\t\t\t}}]}";

        JsonNode query = JsonHandler.getFromString(queryString);
        final UpdateParserMultiple request = new UpdateParserMultiple();
        request.parse(query);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void testExternalUnautorizedUnset() throws InvalidParseOperationException {
        String queryString = "{\"$query\": [],\"$filter\": {},\"$action\": [\n" + "\t\t{\"$unset\": ['_tenant']}]}";

        JsonNode query = JsonHandler.getFromString(queryString);
        final UpdateParserMultiple request = new UpdateParserMultiple();
        request.parse(query);
    }

    @Test
    public void testUpdateParserForNested() throws InvalidParseOperationException {
        final UpdateParserMultiple request = new UpdateParserMultiple();
        request.parse(nestedSearchQuery.deepCopy());
        assertNotNull(request);
    }
}
