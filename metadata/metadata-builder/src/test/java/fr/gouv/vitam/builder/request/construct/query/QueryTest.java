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
package fr.gouv.vitam.builder.request.construct.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERYARGS;
import fr.gouv.vitam.builder.request.construct.query.BooleanQuery;
import fr.gouv.vitam.builder.request.construct.query.CompareQuery;
import fr.gouv.vitam.builder.request.construct.query.ExistsQuery;
import fr.gouv.vitam.builder.request.construct.query.InQuery;
import fr.gouv.vitam.builder.request.construct.query.MatchQuery;
import fr.gouv.vitam.builder.request.construct.query.MltQuery;
import fr.gouv.vitam.builder.request.construct.query.PathQuery;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.builder.request.construct.query.RangeQuery;
import fr.gouv.vitam.builder.request.construct.query.SearchQuery;
import fr.gouv.vitam.builder.request.construct.query.TermQuery;
import fr.gouv.vitam.builder.request.construct.query.WildcardQuery;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;

@SuppressWarnings("javadoc")
public class QueryTest {

    @Test
    public void testRequestBoolean() {
        Query arg1, arg2, argIncomplete;
        try {
            arg1 = new ExistsQuery(QUERY.exists, "var");
            arg2 = new ExistsQuery(QUERY.isNull, "var");
            argIncomplete = new BooleanQuery(QUERY.and);
        } catch (final InvalidCreateOperationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
            return;
        }
        QUERY booleanRequest = QUERY.and;
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
        booleanRequest = QUERY.or;
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
        booleanRequest = QUERY.not;
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
        booleanRequest = QUERY.not;
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
        booleanRequest = QUERY.and;
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
            request = new BooleanQuery(QUERY.eq);
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
            request = new ExistsQuery(QUERY.exists, "var");
            assertTrue(request.isReady());
            request = new ExistsQuery(QUERY.missing, "var");
            assertTrue(request.isReady());
            request = new ExistsQuery(QUERY.isNull, "var");
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new ExistsQuery(QUERY.and, "var");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new ExistsQuery(QUERY.exists, "");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareLong() {
        CompareQuery request = null;
        try {
            request = new CompareQuery(QUERY.lt, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.lte, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gt, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gte, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.eq, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.ne, "var", 1);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.size, "var", 1);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.and, "var", 1);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.lt, "", 1);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareDouble() {
        CompareQuery request = null;
        try {
            request = new CompareQuery(QUERY.lt, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.lte, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gt, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gte, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.eq, "var", 1.0);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.ne, "var", 1.0);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.size, "var", 1.0);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.lt, "", 1.0);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareString() {
        CompareQuery request = null;
        try {
            request = new CompareQuery(QUERY.lt, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.lte, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gt, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gte, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.eq, "var", "val");
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.ne, "var", "val");
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.size, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.lt, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareDate() {
        CompareQuery request = null;
        Date date = new Date(System.currentTimeMillis());
        try {
            request = new CompareQuery(QUERY.lt, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.lte, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gt, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gte, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.eq, "var", date);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.ne, "var", date);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.size, "var", date);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.lt, "", date);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestCompareBoolean() {
        CompareQuery request = null;
        try {
            request = new CompareQuery(QUERY.lt, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.lte, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gt, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.gte, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.eq, "var", true);
            assertTrue(request.isReady());
            request = new CompareQuery(QUERY.ne, "var", true);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new CompareQuery(QUERY.size, "var", true);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new CompareQuery(QUERY.lt, "", true);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestSearch() {
        SearchQuery request = null;
        try {
            request = new SearchQuery(QUERY.regex, "var", "val");
            assertTrue(request.isReady());
            request = new SearchQuery(QUERY.search, "var", "val");
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new SearchQuery(QUERY.size, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new SearchQuery(QUERY.search, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestMatch() {
        MatchQuery request = null;
        try {
            request = new MatchQuery(QUERY.match, "var", "val");
            assertTrue(request.isReady());
            request.setMatchMaxExpansions(10);
            assertTrue(request.getCurrentObject()
                    .has(QUERYARGS.max_expansions.exactToken()));
            request = new MatchQuery(QUERY.match_phrase, "var", "val");
            assertTrue(request.isReady());
            request.setMatchMaxExpansions(10);
            assertTrue(request.getCurrentObject()
                    .has(QUERYARGS.max_expansions.exactToken()));
            request = new MatchQuery(QUERY.match_phrase_prefix, "var", "val");
            assertTrue(request.isReady());
            request.setMatchMaxExpansions(10);
            assertTrue(request.getCurrentObject()
                    .has(QUERYARGS.max_expansions.exactToken()));
            request = new MatchQuery(QUERY.prefix, "var", "val");
            assertTrue(request.isReady());
            request.setMatchMaxExpansions(10);
            assertTrue(request.getCurrentObject()
                    .has(QUERYARGS.max_expansions.exactToken()));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new MatchQuery(QUERY.size, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new MatchQuery(QUERY.match, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestIn() {
        InQuery request = null;
        Date date = new Date(System.currentTimeMillis());
        try {
            request = new InQuery(QUERY.in, "var", true);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.nin, "var", true);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.in, "var", 1);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.nin, "var", 1);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.in, "var", 1.0);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.nin, "var", 1.0);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.in, "var", "val");
            assertTrue(request.isReady());
            request = new InQuery(QUERY.nin, "var", "val");
            assertTrue(request.isReady());
            request = new InQuery(QUERY.in, "var", date);
            assertTrue(request.isReady());
            request = new InQuery(QUERY.nin, "var", date);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new InQuery(QUERY.size, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.in, "", true);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.in, "", 1);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.in, "", 1.0);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.in, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new InQuery(QUERY.in, "", date);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestMlt() {
        MltQuery request = null;
        try {
            request = new MltQuery(QUERY.mlt, "var", "val");
            assertTrue(request.isReady());
            request = new MltQuery(QUERY.flt, "var", "val");
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new MltQuery(QUERY.size, "var", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new MltQuery(QUERY.mlt, "", "val");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestMltMultipleVar() {
        try {
            MltQuery request = new MltQuery(QUERY.mlt, "value", "var1", "var2");
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new MltQuery(QUERY.flt, "value", "var1", "var2");
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
            final MltQuery request = new MltQuery(QUERY.and, "var", "val1", "val2");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            @SuppressWarnings("unused")
            final MltQuery request = new MltQuery(QUERY.mlt, "", "val1", "val2");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestInArray() {
        Date date1 = new Date(System.currentTimeMillis());
        Date date2 = new Date(System.currentTimeMillis() + 1000);
        try {
            InQuery request = null;
            request = new InQuery(QUERY.in, "var", "val1", "val2");
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.nin, "var", "val1", "val2");
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3");
            assertTrue(request.isReady());
            assertEquals(3, request.getCurrentObject().size());
            request.add(1).add(1.0).add(date2);
            assertTrue(request.isReady());
            assertEquals(6, request.getCurrentObject().size());
            request = new InQuery(QUERY.in, "var", 1, 2);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.nin, "var", 1, 2);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3");
            assertTrue(request.isReady());
            assertEquals(5, request.getCurrentObject().size());
            request.add(1).add(1.0).add(date2);
            assertTrue(request.isReady());
            assertEquals(7, request.getCurrentObject().size());
            request = new InQuery(QUERY.in, "var", 1.0, 2.0);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.nin, "var", 1.0, 2.0);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3").add(date2).add(1);
            assertTrue(request.isReady());
            assertEquals(7, request.getCurrentObject().size());
            request.add(1).add(1.0).add(date2).add("val2");
            assertTrue(request.isReady());
            assertEquals(7, request.getCurrentObject().size());
            request = new InQuery(QUERY.in, "var", true, false);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.nin, "var", true, false);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request.add("val1", "val2").add("val3").add(date2);
            assertTrue(request.isReady());
            assertEquals(6, request.getCurrentObject().size());
            request.add(1).add(1.0);
            assertTrue(request.isReady());
            assertEquals(8, request.getCurrentObject().size());
            request = new InQuery(QUERY.in, "var", date1, date2);
            assertTrue(request.isReady());
            assertEquals(2, request.getCurrentObject().size());
            request = new InQuery(QUERY.nin, "var", date1, date2);
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
            final InQuery request = new InQuery(QUERY.and, "var", "val1", "val2");
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRequestTerm() {
        final Map<String, Object> map = new HashMap<String, Object>();
        Date date1 = new Date(System.currentTimeMillis());
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
        Date date1 = new Date(System.currentTimeMillis());
        Date date2 = new Date(System.currentTimeMillis() + 100);
        try {
            request = new RangeQuery("var", QUERY.gt, 1, QUERY.lt, 2);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.gte, 1, QUERY.lte, 2);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.gt, 1.0, QUERY.lt, 2.0);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.gte, 1.0, QUERY.lte, 2.0);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.gt, "1", QUERY.lt, "2");
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.gte, "1", QUERY.lte, "2");
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.gt, date1, QUERY.lt, date2);
            assertTrue(request.isReady());
            request = new RangeQuery("var", QUERY.gte, date1, QUERY.lte, date2);
            assertTrue(request.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // last
        try {
            request = new RangeQuery("var", QUERY.not, 1, QUERY.lt, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new RangeQuery("var", QUERY.lt, 1, QUERY.lt, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new RangeQuery("var", QUERY.lt, 1, QUERY.gt, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new RangeQuery("var", QUERY.gt, 1, QUERY.not, 2);
            fail("Should have raized an exception due to incorrect argument");
        } catch (final InvalidCreateOperationException e) {
            assertNotNull(e);
        }
        try {
            request = new RangeQuery("", QUERY.gt, 1, QUERY.lt, 2);
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
            assertFalse(request.getCurrentQuery().has(QUERYARGS.exactdepth.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.depth.exactToken()));
            request.setExactDepthLimit(1);
            assertTrue(request.getCurrentQuery().has(QUERYARGS.exactdepth.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.depth.exactToken()));
            request.setExactDepthLimit(0);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.exactdepth.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.depth.exactToken()));
            request.setExactDepthLimit(1);
            assertTrue(request.getCurrentQuery().has(QUERYARGS.exactdepth.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.depth.exactToken()));
            request.setExactDepthLimit(0);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.exactdepth.exactToken()));
            assertFalse(request.getCurrentQuery().has(QUERYARGS.depth.exactToken()));
            request.setRelativeDepthLimit(0);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.exactdepth.exactToken()));
            assertTrue(request.getCurrentQuery().has(QUERYARGS.depth.exactToken()));
            request.setRelativeDepthLimit(1);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.exactdepth.exactToken()));
            assertTrue(request.getCurrentQuery().has(QUERYARGS.depth.exactToken()));
            request.setRelativeDepthLimit(-1);
            assertFalse(request.getCurrentQuery().has(QUERYARGS.exactdepth.exactToken()));
            assertTrue(request.getCurrentQuery().has(QUERYARGS.depth.exactToken()));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
