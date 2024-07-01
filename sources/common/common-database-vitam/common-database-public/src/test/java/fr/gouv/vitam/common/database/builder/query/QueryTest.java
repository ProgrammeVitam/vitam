/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.database.builder.query;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERYARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class QueryTest {

    private int size;
    private static final int fakeSize = 1000;

    @Before
    public void setSize() {
        size = GlobalDatas.getLimitValue();
        GlobalDatas.setLimitValue(fakeSize);
    }

    @After
    public void resetSize() {
        GlobalDatas.setLimitValue(size);
    }

    @Test
    public void testRequestBoolean() {
        Query arg1, arg2, argIncomplete;
        try {
            arg1 = new ExistsQuery(QUERY.EXISTS, "var");
            arg2 = new ExistsQuery(QUERY.ISNULL, "var");
            argIncomplete = new BooleanQuery(QUERY.AND);
        } catch (final InvalidCreateOperationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
            return;
        }
        QUERY booleanRequest = QUERY.AND;
        try {
            final BooleanQuery request = new BooleanQuery(booleanRequest);
            assertFalse(request.isReady());
            request.add(arg1).add(arg2).add(arg1, arg2);
            assertTrue(request.isReady());
            assertEquals(4, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        booleanRequest = QUERY.OR;
        try {
            final BooleanQuery request = new BooleanQuery(booleanRequest);
            assertFalse(request.isReady());
            request.add(arg1);
            assertTrue(request.isReady());
            request.add(arg2);
            request.add(arg1, arg2);
            assertTrue(request.isReady());
            assertEquals(4, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        booleanRequest = QUERY.NOT;
        try {
            final BooleanQuery request = new BooleanQuery(booleanRequest);
            assertFalse(request.isReady());
            request.add(arg1);
            assertTrue(request.isReady());
            request.add(arg2);
            request.add(arg1, arg2);
            assertTrue(request.isReady());
            assertEquals(4, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        booleanRequest = QUERY.NOT;
        try {
            final BooleanQuery request = new BooleanQuery(booleanRequest);
            assertFalse(request.isReady());
            request.add(arg1);
            assertTrue(request.isReady());
            request.add(arg2);
            request.add(arg1, arg2);
            assertTrue(request.isReady());
            assertEquals(4, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // Failed tests
        booleanRequest = QUERY.AND;
        BooleanQuery request = null;
        try {
            request = new BooleanQuery(booleanRequest);
            assertFalse(request.isReady());
            request.add(arg1);
            assertTrue(request.isReady());
            assertEquals(1, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            request.add(argIncomplete);
            fail("Should have raized an exception due to incomplete argument");
        } catch (final InvalidCreateOperationException e) {
            assertEquals(1, request.getCurrentObject().size());
        }
        // last
        try {
            request = new BooleanQuery(QUERY.EQ);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestPath() {
        PathQuery request = null;
        try {
            request = new PathQuery("id1", "id2", "id3");
            assertTrue(request.isReady());
            assertEquals(3, request.getCurrentObject().size());
            request.add("id4", "id5").add("id6");
            assertEquals(6, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            request = new PathQuery("");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestExists() {
        ExistsQuery request = null;
        try {
            request = new ExistsQuery(QUERY.EXISTS, "var");
            assertTrue(request.isReady());
            request = new ExistsQuery(QUERY.MISSING, "var");
            assertTrue(request.isReady());
            request = new ExistsQuery(QUERY.ISNULL, "var");
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new ExistsQuery(QUERY.AND, "var");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new ExistsQuery(QUERY.EXISTS, "");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testNoneQuery() throws Exception {
        final NopQuery query = new NopQuery();
        assertTrue(query.isReady());
        assertFalse(query.isFullText());
        assertEquals(QUERY.NOP, query.getQUERY());
        assertEquals(1, query.getCurrentQuery().size());
    }

    @Test
    public void testRequestCompareLong() {
        CompareQuery request = null;
        try {
            request = new CompareQuery(QUERY.LT, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.LTE, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GT, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GTE, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.EQ, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.NE, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.SIZE, "var", 1);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.AND, "var", 1);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.LT, "", 1);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareDouble() {
        CompareQuery request = null;
        try {
            request = new CompareQuery(QUERY.LT, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.LTE, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GT, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GTE, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.EQ, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.NE, "var", 1.0);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.SIZE, "var", 1.0);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.LT, "", 1.0);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareString() {
        CompareQuery request = null;
        try {
            request = new CompareQuery(QUERY.LT, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.LTE, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GT, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GTE, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.EQ, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.NE, "var", "val");
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.SIZE, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.LT, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareDate() {
        CompareQuery request = null;
        final Date date = new Date(System.currentTimeMillis());
        try {
            request = new CompareQuery(QUERY.LT, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.LTE, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GT, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GTE, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.EQ, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.NE, "var", date);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.SIZE, "var", date);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.LT, "", date);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareBoolean() {
        CompareQuery request = null;
        try {
            request = new CompareQuery(QUERY.LT, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.LTE, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GT, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.GTE, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.EQ, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.NE, "var", true);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.SIZE, "var", true);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.LT, "", true);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestSearch() {
        SearchQuery request = null;
        try {
            request = new SearchQuery(QUERY.REGEX, "var", "val");
            assertTrue(request.isReady());
            request = new SearchQuery(QUERY.SEARCH, "var", "val");
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new SearchQuery(QUERY.SIZE, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new SearchQuery(QUERY.SEARCH, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestMatch() {
        MatchQuery request = null;
        try {
            request = new MatchQuery(QUERY.MATCH, "var", "val");
            assertTrue(request.isReady());
            request.setMatchMaxExpansions(10);
            assertTrue(request.getCurrentObject().has(QUERYARGS.MAX_EXPANSIONS.exactToken()));
            request = new MatchQuery(QUERY.MATCH_PHRASE, "var", "val");
            assertTrue(request.isReady());
            request.setMatchMaxExpansions(10);
            assertTrue(request.getCurrentObject().has(QUERYARGS.MAX_EXPANSIONS.exactToken()));
            request = new MatchQuery(QUERY.MATCH_PHRASE_PREFIX, "var", "val");
            assertTrue(request.isReady());
            request.setMatchMaxExpansions(10);
            assertTrue(request.getCurrentObject().has(QUERYARGS.MAX_EXPANSIONS.exactToken()));
            request.setMatchMaxExpansions(10);
            assertTrue(request.getCurrentObject().has(QUERYARGS.MAX_EXPANSIONS.exactToken()));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new MatchQuery(QUERY.SIZE, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new MatchQuery(QUERY.MATCH, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestIn() {
        InQuery request = null;
        final Date date = new Date(System.currentTimeMillis());
        try {
            request = new InQuery(QUERY.IN, "var", true);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.NIN, "var", true);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.IN, "var", 1);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.NIN, "var", 1);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.IN, "var", 1.0);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.NIN, "var", 1.0);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.IN, "var", "val");
            assertTrue(request.isReady());
            request = new InQuery(QUERY.NIN, "var", "val");
            assertTrue(request.isReady());
            request = new InQuery(QUERY.IN, "var", date);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.NIN, "var", date);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new InQuery(QUERY.SIZE, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.IN, "", true);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.IN, "", 1);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.IN, "", 1.0);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.IN, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.IN, "", date);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestMlt() {
        MltQuery request = null;
        try {
            request = new MltQuery(QUERY.MLT, "var", "val");
            assertTrue(request.isReady());
            request = new MltQuery(QUERY.FLT, "var", "val");
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new MltQuery(QUERY.SIZE, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new MltQuery(QUERY.MLT, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestMltMultipleVar() {
        try {
            MltQuery request = new MltQuery(QUERY.MLT, "value", "var1", "var2");
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new MltQuery(QUERY.FLT, "value", "var1", "var2");
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("var1", "var2").add("var3");
            assertTrue(request.isReady());
            assertEquals(3, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            @SuppressWarnings("unused")
            final MltQuery request = new MltQuery(QUERY.AND, "var", "val1", "val2");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            @SuppressWarnings("unused")
            final MltQuery request = new MltQuery(QUERY.MLT, "", "val1", "val2");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestInArray() {
        final Date date1 = new Date(System.currentTimeMillis());
        final Date date2 = new Date(System.currentTimeMillis() + 1000);
        try {
            InQuery request = null;
            request = new InQuery(QUERY.IN, "var", "val1", "val2");
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.NIN, "var", "val1", "val2");
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3");
            assertTrue(request.isReady());
            assertEquals(3, request.getCurrentObject().size());
            request.add(1).add(1.0).add(date2);
            assertTrue(request.isReady());
            assertEquals(6, request.getCurrentObject().size());
            request = new InQuery(QUERY.IN, "var", 1, 2);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.NIN, "var", 1, 2);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3");
            assertTrue(request.isReady());
            assertEquals(5, request.getCurrentObject().size());
            request.add(1).add(1.0).add(date2);
            assertTrue(request.isReady());
            assertEquals(7, request.getCurrentObject().size());
            request = new InQuery(QUERY.IN, "var", 1.0, 2.0);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.NIN, "var", 1.0, 2.0);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3").add(date2).add(1);
            assertTrue(request.isReady());
            assertEquals(7, request.getCurrentObject().size());
            request.add(1).add(1.0).add(date2).add("val2");
            assertTrue(request.isReady());
            assertEquals(7, request.getCurrentObject().size());
            request = new InQuery(QUERY.IN, "var", true, false);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.NIN, "var", true, false);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3").add(date2);
            assertTrue(request.isReady());
            assertEquals(6, request.getCurrentObject().size());
            request.add(1).add(1.0);
            assertTrue(request.isReady());
            assertEquals(8, request.getCurrentObject().size());
            request = new InQuery(QUERY.IN, "var", date1, date2);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.NIN, "var", date1, date2);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3").add(false);
            assertTrue(request.isReady());
            assertEquals(6, request.getCurrentObject().size());
            request.add(1).add(1.0);
            assertTrue(request.isReady());
            assertEquals(8, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            @SuppressWarnings("unused")
            final InQuery request = new InQuery(QUERY.AND, "var", "val1", "val2");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestTerm() {
        final Map<String, Object> map = new HashMap<>();
        final Date date1 = new Date(System.currentTimeMillis());
        map.put("var1", "val1");
        map.put("var2", "val2");
        map.put("var3", date1);
        map.put("var4", 1);
        map.put("var5", 2.0);
        map.put("var6", true);
        TermQuery request = null;
        try {
            request = new TermQuery("var", "val");
            assertTrue(request.isReady());
            request = new TermQuery(map);
            assertEquals(6, request.getCurrentObject().size());
            assertTrue(request.isReady());
            request.add("var2", "val2bis");
            assertTrue(request.isReady());
            assertEquals(6, request.getCurrentObject().size());
            request.add("var3", "val2");
            assertTrue(request.isReady());
            assertEquals(6, request.getCurrentObject().size());
            request.add("var7", "val7");
            assertTrue(request.isReady());
            assertEquals(7, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            request = new TermQuery("", "val1");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestWildcard() {
        WildcardQuery request = null;
        try {
            request = new WildcardQuery("var", "val");
            assertTrue(request.isReady());
            assertEquals(1, request.getCurrentObject().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            request = new WildcardQuery("", "val1");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestRange() {
        RangeQuery request = null;
        final Date date1 = new Date(System.currentTimeMillis());
        final Date date2 = new Date(System.currentTimeMillis() + 100);
        try {
            request = new RangeQuery("var", QUERY.GT, 1, QUERY.LT, 2);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.GTE, 1, QUERY.LTE, 2);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.GT, 1.0, QUERY.LT, 2.0);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.GTE, 1.0, QUERY.LTE, 2.0);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.GT, "1", QUERY.LT, "2");
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.GTE, "1", QUERY.LTE, "2");
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.GT, date1, QUERY.LT, date2);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.GTE, date1, QUERY.LTE, date2);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new RangeQuery("var", QUERY.NOT, 1, QUERY.LT, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new RangeQuery("var", QUERY.LT, 1, QUERY.LT, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new RangeQuery("var", QUERY.LT, 1, QUERY.GT, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new RangeQuery("var", QUERY.GT, 1, QUERY.NOT, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new RangeQuery("", QUERY.GT, 1, QUERY.LT, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testSetDepthLimit() {
        TermQuery request = null;
        try {
            request = new TermQuery("var", "val");
            assertEquals(1, request.getCurrentQuery().size());
            assertTrue(request.isReady());
            assertFalse(request.getCurrentQuery().has(QUERYARGS.EXACTDEPTH.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.DEPTH.exactToken()));
            request.setExactDepthLimit(1);
            assertTrue(request.getCurrentQuery().has(QUERYARGS.EXACTDEPTH.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.DEPTH.exactToken()));
            request.setExactDepthLimit(0);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.EXACTDEPTH.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.DEPTH.exactToken()));
            request.setExactDepthLimit(1);
            assertTrue(request.getCurrentQuery().has(QUERYARGS.EXACTDEPTH.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.DEPTH.exactToken()));
            request.setExactDepthLimit(0);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.EXACTDEPTH.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.DEPTH.exactToken()));
            request.setRelativeDepthLimit(0);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.EXACTDEPTH.exactToken()));
            assertTrue(request.getCurrentQuery().has(QUERYARGS.DEPTH.exactToken()));
            request.setRelativeDepthLimit(1);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.EXACTDEPTH.exactToken()));
            assertTrue(request.getCurrentQuery().has(QUERYARGS.DEPTH.exactToken()));
            request.setRelativeDepthLimit(-1);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.EXACTDEPTH.exactToken()));
            assertTrue(request.getCurrentQuery().has(QUERYARGS.DEPTH.exactToken()));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected String getStringWithLength() {
        final char[] array = new char[fakeSize + 1];
        int pos = 0;
        while (pos < fakeSize + 1) {
            array[pos] = 'a';
            pos++;
        }
        return new String(array);
    }

    @Test
    public void createTermQuery() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", "val");
        assertTrue(request.isReady());
        final TermQuery request2 = new TermQuery("var", new Date(System.currentTimeMillis()));
        assertTrue(request2.isReady());
        final TermQuery request3 = new TermQuery("var", 1);
        assertTrue(request3.isReady());
        final TermQuery request4 = new TermQuery("var", 2.0);
        assertTrue(request4.isReady());
        final TermQuery request5 = new TermQuery("var", true);
        assertTrue(request5.isReady());

        assertEquals(1, request.getCurrentObject().size());
        request.add("var2", 2);
        assertEquals(2, request.getCurrentObject().size());
        request.add("var3", 3.0);
        assertEquals(3, request.getCurrentObject().size());
        request.add("var4", false);
        assertEquals(4, request.getCurrentObject().size());
        request.add("var5", new Date());
        assertEquals(5, request.getCurrentObject().size());
        request.add("var1", "var1");
        assertEquals(6, request.getCurrentObject().size());
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddStringWithEmptyVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        request.add("", "var");
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddIntWithEmptyVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        request.add("", 3);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddDoubleWithEmptyVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        request.add("", 3.0);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddBooleanWithEmptyVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        request.add("", true);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddDateWithEmptyVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        request.add("", new Date(System.currentTimeMillis()));
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddStringWithTooLongVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        final String s = getStringWithLength();
        request.add(s, "var");
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddIntWithTooLongVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        final String s = getStringWithLength();
        request.add(s, 3);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddDoubleWithTooLongVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        final String s = getStringWithLength();
        request.add(s, 3.0);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddBooleanWithTooLongVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        final String s = getStringWithLength();
        request.add(s, true);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddDateWithTooLongVaribaleName() throws InvalidCreateOperationException {
        final TermQuery request = new TermQuery("var", 1);
        final String s = getStringWithLength();
        request.add(s, new Date(System.currentTimeMillis()));
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeIntWithEmptyVariableName() throws InvalidCreateOperationException {
        new RangeQuery("", QUERY.GT, 1, QUERY.LT, 2);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeDoubleWithEmptyVariableName() throws InvalidCreateOperationException {
        new RangeQuery("", QUERY.GT, 1.0, QUERY.LT, 2.0);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeStringWithEmptyVariableName() throws InvalidCreateOperationException {
        new RangeQuery("", QUERY.GT, "1", QUERY.LT, "2");
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeDateWithEmptyVariableName() throws InvalidCreateOperationException {
        final Date date1 = new Date(System.currentTimeMillis());
        final Date date2 = new Date(System.currentTimeMillis() + 100);
        new RangeQuery("", QUERY.GT, date1, QUERY.LT, date2);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeIntWithWrongFromWord() throws InvalidCreateOperationException {
        new RangeQuery("var", QUERY.NOT, 1, QUERY.LT, 2);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeDoubleWithWrongFromWord() throws InvalidCreateOperationException {
        new RangeQuery("var", QUERY.NOT, 1.0, QUERY.LT, 2.0);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeStringWithWrongFromWord() throws InvalidCreateOperationException {
        new RangeQuery("var", QUERY.NOT, "1", QUERY.LT, "2");
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeDateWithWrongFromWord() throws InvalidCreateOperationException {
        final Date date1 = new Date(System.currentTimeMillis());
        final Date date2 = new Date(System.currentTimeMillis() + 100);
        new RangeQuery("var", QUERY.NOT, date1, QUERY.LT, date2);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeIntWithWrongToWord() throws InvalidCreateOperationException {
        new RangeQuery("var", QUERY.GT, 1, QUERY.NOT, 2);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeDoubleWithWrongToWord() throws InvalidCreateOperationException {
        new RangeQuery("var", QUERY.GT, 1.0, QUERY.NOT, 2.0);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeStringWithWrongToWord() throws InvalidCreateOperationException {
        new RangeQuery("var", QUERY.GT, "1", QUERY.NOT, "2");
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenAddRangeDateWithToFromWord() throws InvalidCreateOperationException {
        final Date date1 = new Date(System.currentTimeMillis());
        final Date date2 = new Date(System.currentTimeMillis() + 100);
        new RangeQuery("var", QUERY.GT, date1, QUERY.NOT, date2);
    }

    @Test
    public void testClean() throws InvalidCreateOperationException {
        final BooleanQuery bq = new BooleanQuery(QUERY.AND);
        final MltQuery mq = new MltQuery(QUERY.MLT, "var", "val");
        final InQuery iq = new InQuery(QUERY.IN, "var", true);
        bq.clean();
        assertEquals(0, bq.getCurrentObject().size());
        mq.clean();
        assertEquals(0, mq.getCurrentObject().size());
        iq.clean();
        assertEquals(0, iq.getCurrentObject().size());
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenInRequestAddBooleanWithTooLongVariableName()
        throws InvalidCreateOperationException {
        new InQuery(QUERY.IN, getStringWithLength(), true);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenInRequestAddIntWithTooLongVariableName()
        throws InvalidCreateOperationException {
        new InQuery(QUERY.IN, getStringWithLength(), 1);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenInRequestAddDoubleWithTooLongVariableName()
        throws InvalidCreateOperationException {
        new InQuery(QUERY.IN, getStringWithLength(), 1.0);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenInRequestAddDateWithTooLongVariableName()
        throws InvalidCreateOperationException {
        final Date date = new Date(System.currentTimeMillis());
        new InQuery(QUERY.IN, getStringWithLength(), date);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenInRequestAddBooleanWithWrongQueryWord() throws InvalidCreateOperationException {
        new InQuery(QUERY.GT, getStringWithLength(), true);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenInRequestAddIntWithWrongQueryWord() throws InvalidCreateOperationException {
        new InQuery(QUERY.GT, getStringWithLength(), 1);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenInRequestAddDoubleWithWrongQueryWord() throws InvalidCreateOperationException {
        new InQuery(QUERY.GT, getStringWithLength(), 1.0);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenInRequestAddDateWithWrongQueryWord() throws InvalidCreateOperationException {
        final Date date = new Date(System.currentTimeMillis());
        final String s = getStringWithLength();
        new InQuery(QUERY.GT, s, date);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenRequestMltWithTooLongValue() throws InvalidCreateOperationException {
        final String s = getStringWithLength();
        new MltQuery(QUERY.MLT, s, "var");
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void shouldRaiseExceptionWhenRequestMltWithTooLongVariableName() throws InvalidCreateOperationException {
        final String s = getStringWithLength();
        new MltQuery(QUERY.MLT, "var", s);
    }
}
