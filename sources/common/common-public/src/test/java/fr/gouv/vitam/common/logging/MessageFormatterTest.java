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
package fr.gouv.vitam.common.logging;

import fr.gouv.vitam.common.ResourcesPublicUtilTest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MessageFormatterTest {

    @Test
    public void testGlobal() {
        boolean boolean0 = MessageFormatter.isDoubleEscaped("ENUM$VALUES", 5);
        assertFalse(boolean0);
        FormattingTuple formattingTuple0 = MessageFormatter.format("{}", (Object) null, "{}");
        assertNotNull(formattingTuple0);
        boolean0 = MessageFormatter.isEscapedDelimeter("6J]Ulo:L>", 3);
        assertFalse(boolean0);
        Object[] objectArray0 = new Object[5];
        final Throwable throwable0 = MessageFormatter.getThrowableCandidate(objectArray0);
        assertNull(throwable0);
        boolean0 = MessageFormatter.isDoubleEscaped("6J]Ulo:L>", 4);
        assertFalse(boolean0);
        boolean0 = MessageFormatter.isDoubleEscaped("R!{}", -918);
        assertFalse(boolean0);
        objectArray0 = new Object[5];
        formattingTuple0 = MessageFormatter.arrayFormat("{}", objectArray0);
        assertNotNull(formattingTuple0);
        formattingTuple0 = MessageFormatter.format((String) null, (Object) null);
        assertNotNull(formattingTuple0);
        objectArray0 = new Object[0];
        formattingTuple0 = MessageFormatter.arrayFormat("6J]Ulo:L>", objectArray0);
        assertNotNull(formattingTuple0);
        formattingTuple0 = MessageFormatter.arrayFormat("[FAILED toString()]", (Object[]) null);
        assertNotNull(formattingTuple0);
        formattingTuple0 = MessageFormatter.format("", "");
        assertNotNull(formattingTuple0);
        objectArray0 = new Object[5];
        objectArray0[0] = "R!{}";
        formattingTuple0 = MessageFormatter.arrayFormat("R!{}", objectArray0);
        assertNotNull(formattingTuple0);
        formattingTuple0 = MessageFormatter.arrayFormat("[FAILED toString()]", new Boolean[1]);
        assertNotNull(formattingTuple0);
    }

    @Test
    public void testAppendParameter() {
        final StringBuilder sbuild = new StringBuilder();
        final Map<Object[], Void> seenMap = new HashMap<>();

        final boolean[] ob = new boolean[] { true };
        MessageFormatter.deeplyAppendParameter(sbuild, ob, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);

        final byte[] ob2 = new byte[] { (byte) 1 };
        MessageFormatter.deeplyAppendParameter(sbuild, ob2, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);

        final char[] oc = new char[] { 'a' };
        MessageFormatter.deeplyAppendParameter(sbuild, oc, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);

        final short[] os = new short[] { (short) 1 };
        MessageFormatter.deeplyAppendParameter(sbuild, os, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);

        final int[] oi = new int[] { 1 };
        MessageFormatter.deeplyAppendParameter(sbuild, oi, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);

        final long[] ol = new long[] { 1L };
        MessageFormatter.deeplyAppendParameter(sbuild, ol, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);

        final float[] of = new float[] { (float) 1.5 };
        MessageFormatter.deeplyAppendParameter(sbuild, of, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);

        final double[] od = new double[] { 2.5 };
        MessageFormatter.deeplyAppendParameter(sbuild, od, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);

        final Object[] ob3 = new Object[] { new Object() };
        MessageFormatter.deeplyAppendParameter(sbuild, ob3, seenMap);
        assertTrue(sbuild.length() > 0);
        sbuild.setLength(0);
    }

    @Test
    public void testError() {
        try {
            MessageFormatter.isEscapedDelimeter((String) null, -1);
            fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            MessageFormatter.isDoubleEscaped((String) null, 163);
            fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
    }
}
