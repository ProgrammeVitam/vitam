package fr.gouv.vitam.parser.request.parser.query;

import static fr.gouv.vitam.parser.request.parser.query.QueryParserHelper.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.parser.request.parser.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

@SuppressWarnings("javadoc")
public class QueryParserHelperTest {

    VarNameAdapter noAdapter = new VarNameAdapter();
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    private void compare(Query query, Query request2) {
        assertEquals("String shall be equal", query.getCurrentQuery().toString(),
                request2.getCurrentQuery().toString());
    }

    @Test
    public void testPathJsonNode() {
        try {
            Query path = path("id1", "id2");
            Query path2 = path(path.getNode(QUERY.path.exactToken()), noAdapter);
            compare(path, path2);
        } catch (InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testCompareJsonNode() {
        try {
            Query comp = eq("var1", true);
            Query comp2 = eq(comp.getNode(QUERY.eq.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = eq("var1", 10);
            comp2 = eq(comp.getNode(QUERY.eq.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = eq("var1", 10.5);
            comp2 = eq(comp.getNode(QUERY.eq.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = eq("var1", "value");
            comp2 = eq(comp.getNode(QUERY.eq.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = eq("var1", new Date());
            comp2 = eq(comp.getNode(QUERY.eq.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = ne("var1", 10);
            comp2 = ne(comp.getNode(QUERY.ne.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = lt("var1", 10);
            comp2 = lt(comp.getNode(QUERY.lt.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = lte("var1", 10);
            comp2 = lte(comp.getNode(QUERY.lte.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = gt("var1", 10);
            comp2 = gt(comp.getNode(QUERY.gt.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = gte("var1", 10);
            comp2 = gte(comp.getNode(QUERY.gte.exactToken()), noAdapter);
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
            Query comp2 = in(comp.getNode(QUERY.in.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = in("var1", 10, 5);
            comp2 = in(comp.getNode(QUERY.in.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = in("var1", 10.5, 12.6);
            comp2 = in(comp.getNode(QUERY.in.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = in("var1", "value", "value2");
            comp2 = in(comp.getNode(QUERY.in.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = in("var1", new Date(), new Date(10));
            comp2 = in(comp.getNode(QUERY.in.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = nin("var1", 10, 20);
            comp2 = nin(comp.getNode(QUERY.nin.exactToken()), noAdapter);
            compare(comp, comp2);
        } catch (InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMatchJsonNode() {
        try {
            Query match = match("var", "value").setMatchMaxExpansions(5);
            Query match2 = match(match.getNode(QUERY.match.exactToken()), noAdapter);
            compare(match, match2);
            match = matchPhrase("var", "value");
            match2 = matchPhrase(match.getNode(QUERY.match_phrase.exactToken()), noAdapter);
            compare(match, match2);
            match = matchPhrasePrefix("var", "value");
            match2 = matchPhrasePrefix(match.getNode(QUERY.match_phrase_prefix.exactToken()), noAdapter);
            compare(match, match2);
            match = prefix("var", "value");
            match2 = prefix(match.getNode(QUERY.prefix.exactToken()), noAdapter);
            compare(match, match2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testRegexJsonNode() {
        try {
            Query reg = regex("id1", "id2");
            Query reg2 = regex(reg.getNode(QUERY.regex.exactToken()), noAdapter);
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
            Query comp2 = term(comp.getNode(QUERY.term.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = term("var1", 10);
            comp2 = term(comp.getNode(QUERY.term.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = term("var1", 10.5);
            comp2 = term(comp.getNode(QUERY.term.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = term("var1", "value");
            comp2 = term(comp.getNode(QUERY.term.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = term("var1", new Date());
            comp2 = term(comp.getNode(QUERY.term.exactToken()), noAdapter);
            compare(comp, comp2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testWildcardJsonNode() {
        try {
            Query wildcard = wildcard("var", "value");
            Query wildcard2 =
                    wildcard(wildcard.getNode(QUERY.wildcard.exactToken()), noAdapter);
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
            Query flt2 = flt(flt.getNode(QUERY.flt.exactToken()), noAdapter);
            compare(flt, flt2);
            flt = mlt("value", "var1", "var2");
            flt2 = mlt(flt.getNode(QUERY.mlt.exactToken()), noAdapter);
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
            Query comp2 = range(comp.getNode(QUERY.range.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = range("var1", 10.5, false, 20.5, true);
            comp2 = range(comp.getNode(QUERY.range.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = range("var1", "value", false, "value2", false);
            comp2 = range(comp.getNode(QUERY.range.exactToken()), noAdapter);
            compare(comp, comp2);
            comp = range("var1", new Date(), true, new Date(10000000L), false);
            comp2 = range(comp.getNode(QUERY.range.exactToken()), noAdapter);
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
            Query bool2 = and().add(wildcard(
                    bool.getCurrentObject().get(0).get(QUERY.wildcard.exactToken()), noAdapter));
            compare(bool, bool2);
            bool = or().add(wildcard("var", "value"));
            bool2 = or().add(wildcard(
                    bool.getCurrentObject().get(0).get(QUERY.wildcard.exactToken()), noAdapter));
            compare(bool, bool2);
            bool = not().add(wildcard("var", "value"));
            bool2 = not().add(wildcard(
                    bool.getCurrentObject().get(0).get(QUERY.wildcard.exactToken()), noAdapter));
            compare(bool, bool2);
            bool = not().add(wildcard("var", "value"));
            bool2 = not().add(wildcard(
                    bool.getCurrentObject().get(0).get(QUERY.wildcard.exactToken()), noAdapter));
            compare(bool, bool2);
            bool = not().add(wildcard("var", "value"), wildcard("var2", "value2"));
            bool2 = not().add(wildcard(
                    bool.getCurrentObject().get(0).get(QUERY.wildcard.exactToken()), noAdapter))
                    .add(wildcard(bool.getCurrentObject().get(1)
                            .get(QUERY.wildcard.exactToken()), noAdapter));
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
            Query exists2 = exists(
                    exists.getNode(QUERY.exists.exactToken()), noAdapter);
            compare(exists, exists2);
            exists = missing("var1");
            exists2 = missing(
                    exists.getNode(QUERY.missing.exactToken()), noAdapter);
            compare(exists, exists2);
            exists = isNull("var1");
            exists2 = isNull(
                    exists.getNode(QUERY.isNull.exactToken()), noAdapter);
            compare(exists, exists2);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
