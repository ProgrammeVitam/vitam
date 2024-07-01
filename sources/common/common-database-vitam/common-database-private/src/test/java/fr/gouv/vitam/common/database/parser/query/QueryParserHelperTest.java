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
package fr.gouv.vitam.common.database.parser.query;

import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

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
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.range;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.regex;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.term;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.wildcard;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.eq;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.exists;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.flt;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.gt;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.gte;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.in;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.isNull;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.lt;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.lte;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.match;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.matchPhrase;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.matchPhrasePrefix;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.missing;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.mlt;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.ne;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.nin;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.nop;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.path;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.range;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.regex;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.term;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.wildcard;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class QueryParserHelperTest {

    VarNameAdapter noAdapter = new VarNameAdapter();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    private void compare(Query query, Query request2) {
        assertEquals(
            "String shall be equal",
            query.getCurrentQuery().toString(),
            request2.getCurrentQuery().toString()
        );
    }

    @Test
    public void testPathJsonNode() {
        try {
            final Query path = path("id1", "id2");
            final Query path2 = path(path.getNode(QUERY.PATH.exactToken()), noAdapter);
            compare(path, path2);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testCompareJsonNode() {
        try {
            Query comp = eq("var1", true);
            Query comp2 = eq(comp.getNode(QUERY.EQ.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = eq("var1", 10);
            comp2 = eq(comp.getNode(QUERY.EQ.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = eq("var1", 10.5);
            comp2 = eq(comp.getNode(QUERY.EQ.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = eq("var1", "value");
            comp2 = eq(comp.getNode(QUERY.EQ.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = eq("var1", new Date());
            comp2 = eq(comp.getNode(QUERY.EQ.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = ne("var1", 10);
            comp2 = ne(comp.getNode(QUERY.NE.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = lt("var1", 10);
            comp2 = lt(comp.getNode(QUERY.LT.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = lte("var1", 10);
            comp2 = lte(comp.getNode(QUERY.LTE.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = gt("var1", 10);
            comp2 = gt(comp.getNode(QUERY.GT.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = gte("var1", 10);
            comp2 = gte(comp.getNode(QUERY.GTE.exactToken()), noAdapter);
            compare(comp, comp2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testInJsonNode() {
        try {
            Query comp = in("var1", true, false);
            Query comp2 = in(comp.getNode(QUERY.IN.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = in("var1", 10, 5);
            comp2 = in(comp.getNode(QUERY.IN.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = in("var1", 10.5, 12.6);
            comp2 = in(comp.getNode(QUERY.IN.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = in("var1", "value", "value2");
            comp2 = in(comp.getNode(QUERY.IN.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = in("var1", new Date(), new Date(10));
            comp2 = in(comp.getNode(QUERY.IN.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = nin("var1", 10, 20);
            comp2 = nin(comp.getNode(QUERY.NIN.exactToken()), noAdapter);
            compare(comp, comp2);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMatchJsonNode() {
        try {
            Query match = match("var", "value").setMatchMaxExpansions(5);
            Query match2 = match(match.getNode(QUERY.MATCH.exactToken()), noAdapter);
            compare(match, match2);
            match = matchPhrase("var", "value");
            match2 = matchPhrase(match.getNode(QUERY.MATCH_PHRASE.exactToken()), noAdapter);
            compare(match, match2);
            match = matchPhrasePrefix("var", "value");
            match2 = matchPhrasePrefix(match.getNode(QUERY.MATCH_PHRASE_PREFIX.exactToken()), noAdapter);
            compare(match, match2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testRegexJsonNode() {
        try {
            final Query reg = regex("id1", "id2");
            final Query reg2 = regex(reg.getNode(QUERY.REGEX.exactToken()), noAdapter);
            compare(reg, reg2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testTermJsonNode() {
        try {
            Query comp = term("var1", true);
            Query comp2 = term(comp.getNode(QUERY.TERM.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = term("var1", 10);
            comp2 = term(comp.getNode(QUERY.TERM.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = term("var1", 10.5);
            comp2 = term(comp.getNode(QUERY.TERM.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = term("var1", "value");
            comp2 = term(comp.getNode(QUERY.TERM.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = term("var1", new Date());
            comp2 = term(comp.getNode(QUERY.TERM.exactToken()), noAdapter);
            compare(comp, comp2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testWildcardJsonNode() {
        try {
            final Query wildcard = wildcard("var", "value");
            final Query wildcard2 = wildcard(wildcard.getNode(QUERY.WILDCARD.exactToken()), noAdapter);
            compare(wildcard, wildcard2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFltJsonNode() {
        try {
            Query flt = flt("value", "var1", "var2");
            Query flt2 = flt(flt.getNode(QUERY.FLT.exactToken()), noAdapter);
            compare(flt, flt2);
            flt = mlt("value", "var1", "var2");
            flt2 = mlt(flt.getNode(QUERY.MLT.exactToken()), noAdapter);
            compare(flt, flt2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testRangeJsonNode() {
        try {
            Query comp = range("var1", 10, true, 20, false);
            Query comp2 = range(comp.getNode(QUERY.RANGE.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = range("var1", 10.5, false, 20.5, true);
            comp2 = range(comp.getNode(QUERY.RANGE.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = range("var1", "value", false, "value2", false);
            comp2 = range(comp.getNode(QUERY.RANGE.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = range("var1", new Date(), true, new Date(10000000L), false);
            comp2 = range(comp.getNode(QUERY.RANGE.exactToken()), noAdapter);
            compare(comp, comp2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testBooleanJsonNode() {
        try {
            Query bool = and().add(wildcard("var", "value"));
            Query bool2 = and()
                .add(wildcard(bool.getCurrentObject().get(0).get(QUERY.WILDCARD.exactToken()), noAdapter));
            compare(bool, bool2);
            bool = or().add(wildcard("var", "value"));
            bool2 = or().add(wildcard(bool.getCurrentObject().get(0).get(QUERY.WILDCARD.exactToken()), noAdapter));
            compare(bool, bool2);
            bool = not().add(wildcard("var", "value"));
            bool2 = not().add(wildcard(bool.getCurrentObject().get(0).get(QUERY.WILDCARD.exactToken()), noAdapter));
            compare(bool, bool2);
            bool = not().add(wildcard("var", "value"));
            bool2 = not().add(wildcard(bool.getCurrentObject().get(0).get(QUERY.WILDCARD.exactToken()), noAdapter));
            compare(bool, bool2);
            bool = not().add(wildcard("var", "value"), wildcard("var2", "value2"));
            bool2 = not()
                .add(wildcard(bool.getCurrentObject().get(0).get(QUERY.WILDCARD.exactToken()), noAdapter))
                .add(wildcard(bool.getCurrentObject().get(1).get(QUERY.WILDCARD.exactToken()), noAdapter));
            compare(bool, bool2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testExistJsonNode() {
        try {
            Query exists = exists("var1");
            Query exists2 = exists(exists.getNode(QUERY.EXISTS.exactToken()), noAdapter);
            compare(exists, exists2);
            exists = missing("var1");
            exists2 = missing(exists.getNode(QUERY.MISSING.exactToken()), noAdapter);
            compare(exists, exists2);
            exists = isNull("var1");
            exists2 = isNull(exists.getNode(QUERY.ISNULL.exactToken()), noAdapter);
            compare(exists, exists2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testNopQuery() throws Exception {
        final Query nop = nop();
        assertEquals(1, nop.getCurrentQuery().size());
        assertEquals("{\"$nop\":\"1\"}", JsonHandler.unprettyPrint(nop.getCurrentQuery()));
        assertEquals("{\"$nop\":\"1\"}", JsonHandler.unprettyPrint(nop.getCurrentObject()));
        assertEquals(QUERY.NOP, nop.getQUERY());
    }
}
