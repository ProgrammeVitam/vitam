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

import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.search;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.size;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.term;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.wildcard;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class QueryHelperTest {

    private int limitValue;
    private int limitParameter;

    private static String createLongString(int size) {
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Before
    public void setupConfig() {
        limitValue = GlobalDatas.getLimitValue();
        limitParameter = GlobalDatas.getLimitParameter();
        GlobalDatas.setLimitValue(1000);
        GlobalDatas.setLimitParameter(100);
    }

    @After
    public void tearDown() {
        GlobalDatas.setLimitValue(limitValue);
        GlobalDatas.setLimitParameter(limitParameter);
    }

    @Test
    public void testSanityCheckRequest() {
        try {
            final String longname = createLongString(GlobalDatas.getLimitParameter() + 100);
            path(longname, "id2");
            fail("Should fail");
        } catch (final InvalidCreateOperationException e) {}
        try {
            final String longvalue = createLongString(GlobalDatas.getLimitValue() + 100);
            eq("var", longvalue);
            fail("Should fail");
        } catch (final InvalidCreateOperationException e) {}
        try {
            eq("_var", "val");
            fail("Should fail");
        } catch (final InvalidCreateOperationException e) {}
    }

    @Test
    public void testPathRequest() {
        try {
            final Query query = path("id1", "id2");
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testBoolRequest() {
        try {
            Query query = and().add(eq("var", "val"));
            assertTrue(query.isReady());
            query = or().add(eq("var", "val"));
            assertTrue(query.isReady());
            query = not().add(eq("var", "val"));
            assertTrue(query.isReady());
            query = not().add(eq("var", "val"));
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testCompareRequest() {
        final Date date1 = new Date(System.currentTimeMillis());
        try {
            Query query = eq("var", true);
            assertTrue(query.isReady());
            query = eq("var", 1);
            assertTrue(query.isReady());
            query = eq("var", 1.0);
            assertTrue(query.isReady());
            query = eq("var", "val");
            assertTrue(query.isReady());
            query = eq("var", date1);
            assertTrue(query.isReady());

            query = ne("var", true);
            assertTrue(query.isReady());
            query = ne("var", 1);
            assertTrue(query.isReady());
            query = ne("var", 1.0);
            assertTrue(query.isReady());
            query = ne("var", "val");
            assertTrue(query.isReady());
            query = ne("var", date1);
            assertTrue(query.isReady());

            query = gt("var", true);
            assertTrue(query.isReady());
            query = gt("var", 1);
            assertTrue(query.isReady());
            query = gt("var", 1.0);
            assertTrue(query.isReady());
            query = gt("var", "val");
            assertTrue(query.isReady());
            query = gt("var", date1);
            assertTrue(query.isReady());

            query = gte("var", true);
            assertTrue(query.isReady());
            query = gte("var", 1);
            assertTrue(query.isReady());
            query = gte("var", 1.0);
            assertTrue(query.isReady());
            query = gte("var", "val");
            assertTrue(query.isReady());
            query = gte("var", date1);
            assertTrue(query.isReady());

            query = lt("var", true);
            assertTrue(query.isReady());
            query = lt("var", 1);
            assertTrue(query.isReady());
            query = lt("var", 1.0);
            assertTrue(query.isReady());
            query = lt("var", "val");
            assertTrue(query.isReady());
            query = lt("var", date1);
            assertTrue(query.isReady());

            query = lte("var", true);
            assertTrue(query.isReady());
            query = lte("var", 1);
            assertTrue(query.isReady());
            query = lte("var", 1.0);
            assertTrue(query.isReady());
            query = lte("var", "val");
            assertTrue(query.isReady());
            query = lte("var", date1);
            assertTrue(query.isReady());

            query = size("var", 1);
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testExistsRequest() {
        try {
            Query query = exists("var");
            assertTrue(query.isReady());

            query = missing("var");
            assertTrue(query.isReady());

            query = isNull("var");
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testInRequest() {
        final Date date1 = new Date(System.currentTimeMillis());
        final Date date2 = new Date(System.currentTimeMillis() + 100);
        try {
            Query query = in("var", true);
            assertTrue(query.isReady());
            query = in("var", true, true);
            assertTrue(query.isReady());
            query = in("var", 1);
            assertTrue(query.isReady());
            query = in("var", 1, 1);
            assertTrue(query.isReady());
            query = in("var", 1.0);
            assertTrue(query.isReady());
            query = in("var", 1.0, 1.0);
            assertTrue(query.isReady());
            query = in("var", "val");
            assertTrue(query.isReady());
            query = in("var", "val", "val");
            assertTrue(query.isReady());
            query = in("var", date1);
            assertTrue(query.isReady());
            query = in("var", date2);
            assertTrue(query.isReady());

            query = nin("var", true);
            assertTrue(query.isReady());
            query = nin("var", true, true);
            assertTrue(query.isReady());
            query = nin("var", 1);
            assertTrue(query.isReady());
            query = nin("var", 1, 1);
            assertTrue(query.isReady());
            query = nin("var", 1.0);
            assertTrue(query.isReady());
            query = nin("var", 1.0, 1.0);
            assertTrue(query.isReady());
            query = nin("var", "val");
            assertTrue(query.isReady());
            query = nin("var", "val", "val");
            assertTrue(query.isReady());
            query = nin("var", date1);
            assertTrue(query.isReady());
            query = nin("var", date2);
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMatchRequest() {
        try {
            Query query = match("var", "value");
            assertTrue(query.isReady());
            query = matchPhrase("var", "value");
            assertTrue(query.isReady());
            query = matchPhrasePrefix("var", "value");
            assertTrue(query.isReady());
            query = regex("var", "value");
            assertTrue(query.isReady());
            query = search("var", "value");
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testTermRequest() {
        try {
            Query query = term("var", "value");
            assertTrue(query.isReady());
            final Map<String, Object> map = new HashMap<>();
            map.put("var1", "val1");
            map.put("var2", "val2");
            map.put("var3", new Date(0));
            map.put("var4", 1);
            map.put("var5", 2.0);
            map.put("var6", true);
            query = term(map);
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testSearchRequest() {
        try {
            Query query = regex("var", "value");
            assertTrue(query.isReady());
            query = search("var", "value");
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testWildcardRequest() {
        try {
            final Query query = wildcard("var", "value");
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMltRequest() {
        try {
            Query query = flt("value", "var");
            assertTrue(query.isReady());
            query = flt("value", "var1", "var2");
            assertTrue(query.isReady());

            query = mlt("value", "var");
            assertTrue(query.isReady());
            query = mlt("value", "var1", "var2");
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testRangeRequest() {
        final Date date1 = new Date(System.currentTimeMillis());
        final Date date2 = new Date(System.currentTimeMillis() + 100);
        try {
            Query query = range("var", 1, false, 2, false);
            assertTrue(query.isReady());
            query = range("var", 1, true, 2, false);
            assertTrue(query.isReady());
            query = range("var", 1, false, 2, true);
            assertTrue(query.isReady());
            query = range("var", 1, true, 2, true);
            assertTrue(query.isReady());

            query = range("var", 1.0, false, 2.0, false);
            assertTrue(query.isReady());
            query = range("var", 1.0, true, 2.0, false);
            assertTrue(query.isReady());
            query = range("var", 1.0, false, 2.0, true);
            assertTrue(query.isReady());
            query = range("var", 1.0, true, 2.0, true);
            assertTrue(query.isReady());

            query = range("var", "1", false, "2", false);
            assertTrue(query.isReady());
            query = range("var", "1", true, "2", false);
            assertTrue(query.isReady());
            query = range("var", "1", false, "2", true);
            assertTrue(query.isReady());
            query = range("var", "1", true, "2", true);
            assertTrue(query.isReady());

            query = range("var", date1, false, date2, false);
            assertTrue(query.isReady());
            query = range("var", date1, true, date2, false);
            assertTrue(query.isReady());
            query = range("var", date1, false, date2, true);
            assertTrue(query.isReady());
            query = range("var", date1, true, date2, true);
            assertTrue(query.isReady());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
