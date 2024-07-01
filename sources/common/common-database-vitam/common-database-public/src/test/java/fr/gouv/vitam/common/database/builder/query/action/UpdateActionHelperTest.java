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
package fr.gouv.vitam.common.database.builder.query.action;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class UpdateActionHelperTest {

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
            add(longname, "id2");
            fail("Should fail");
        } catch (final InvalidCreateOperationException e) {}
        try {
            final String longvalue = createLongString(GlobalDatas.getLimitValue() + 100);
            add("var", longvalue);
            fail("Should fail");
        } catch (final InvalidCreateOperationException e) {}
    }

    @Test
    public void testAdd() {
        try {
            Action action = add("var1", "val1", "val2");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = add("var1", 1, 2);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = add("var1", 1.0, 2.0);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = add("var1", true, false);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            ((AddAction) action).add(true, false, true);
            assertTrue(action.getCurrentObject().size() == 5);
            ((AddAction) action).add(3, 4, 5);
            assertTrue(action.getCurrentObject().size() == 8);
            ((AddAction) action).add(3.0, 4.0, 5.0);
            assertTrue(action.getCurrentObject().size() == 11);
            ((AddAction) action).add("val1", "val2", "val3");
            assertTrue(action.getCurrentObject().size() == 14);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testInc() {
        try {
            Action action = inc("var1");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asInt() == 1);
            action = inc("var1", 5);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asInt() == 5);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMin() throws Exception {
        try {
            Action action = min("var1", 5);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asInt() == 5);
            action = min("var1", 5.2);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asDouble() == 5.2);
            action = min("var1", false);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asBoolean() == false);
            action = min("var1", "val");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asText() == "val");
            final Date date = new Date(0);
            action = min("var1", date);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            final Date date2 = LocalDateUtil.getDate(action.getCurrentObject().path("var1").get(Query.DATE).asText());
            assertTrue(date.equals(date2));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMax() throws Exception {
        try {
            Action action = max("var1", 5);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asInt() == 5);
            action = max("var1", 5.2);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asDouble() == 5.2);
            action = max("var1", false);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asBoolean() == false);
            action = max("var1", "val");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asText() == "val");
            final Date date = new Date(0);
            action = max("var1", date);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            final Date date2 = LocalDateUtil.getDate(action.getCurrentObject().path("var1").get(Query.DATE).asText());
            assertTrue(date.equals(date2));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testPull() {
        try {
            Action action = pull("var1", "val1", "val2");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = pull("var1", 1, 2);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = pull("var1", 1.0, 2.0);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = pull("var1", new Date(), new Date(0));
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = pull("var1", true, false);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            ((PullAction) action).add(true, false, true);
            assertTrue(action.getCurrentObject().size() == 5);
            ((PullAction) action).add(3, 4, 5);
            assertTrue(action.getCurrentObject().size() == 8);
            ((PullAction) action).add(3.0, 4.0, 5.0);
            assertTrue(action.getCurrentObject().size() == 11);
            ((PullAction) action).add("val1", "val2", "val3");
            assertTrue(action.getCurrentObject().size() == 14);
            ((PullAction) action).add(new Date(), new Date(0));
            assertTrue(action.getCurrentObject().size() == 16);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testPop() {
        try {
            Action action = pop("var1");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asInt() == 1);
            action = pop("var1", 1);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asInt() == 1);
            action = pop("var1", -1);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            assertTrue(action.getCurrentObject().path("var1").asInt() == -1);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testPush() {
        try {
            Action action = push("var1", "val1", "val2");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = push("var1", 1, 2);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = push("var1", 1.0, 2.0);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = push("var1", new Date(), new Date(0));
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            action = push("var1", true, false);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 2);
            ((PushAction) action).add(true, false, true);
            assertTrue(action.getCurrentObject().size() == 5);
            ((PushAction) action).add(3, 4, 5);
            assertTrue(action.getCurrentObject().size() == 8);
            ((PushAction) action).add(3.0, 4.0, 5.0);
            assertTrue(action.getCurrentObject().size() == 11);
            ((PushAction) action).add("val1", "val2", "val3");
            assertTrue(action.getCurrentObject().size() == 14);
            ((PushAction) action).add(new Date(), new Date(0));
            assertTrue(action.getCurrentObject().size() == 16);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testRename() {
        try {
            final Action action = rename("var1", "var2");
            assertTrue(action.getCurrentAction().size() == 1);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testSet() {
        try {
            Action action = set("var1", "val1");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            final HashMap<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);
            action = set(map);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 3);
            action = set("var1", 1);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            action = set("var1", 1.0);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            action = set("var1", new Date());
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            action = set("var1", true);
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            ((SetAction) action).add("var2", false);
            assertTrue(action.getCurrentObject().size() == 2);
            ((SetAction) action).add("var3", 5);
            assertTrue(action.getCurrentObject().size() == 3);
            ((SetAction) action).add("var4", 5.0);
            assertTrue(action.getCurrentObject().size() == 4);
            ((SetAction) action).add("var5", "val3");
            assertTrue(action.getCurrentObject().size() == 5);
            ((SetAction) action).add("var6", new Date());
            assertTrue(action.getCurrentObject().size() == 6);
            ((SetAction) action).add("var2", false);
            assertTrue(action.getCurrentObject().size() == 6);
            ((SetAction) action).add("var3", 5);
            assertTrue(action.getCurrentObject().size() == 6);
            ((SetAction) action).add("var4", 5.0);
            assertTrue(action.getCurrentObject().size() == 6);
            ((SetAction) action).add("var5", "val3");
            assertTrue(action.getCurrentObject().size() == 6);
            ((SetAction) action).add("var6", new Date());
            assertTrue(action.getCurrentObject().size() == 6);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testUnset() {
        try {
            final Action action = unset("var1");
            assertTrue(action.getCurrentAction().size() == 1);
            assertTrue(action.getCurrentObject().size() == 1);
            ((UnsetAction) action).add("var5", "var3");
            assertTrue(action.getCurrentObject().size() == 3);
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
