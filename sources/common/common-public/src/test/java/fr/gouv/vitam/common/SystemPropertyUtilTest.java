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
package fr.gouv.vitam.common;

import fr.gouv.vitam.common.SystemPropertyUtil.Platform;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SystemPropertyUtilTest {

    private static final String OS_NAME = "os.name";
    private static final String OTHER = "other";
    private static final String KEY_TEST = "keyTest";
    private static final String KEY_ITEST = "keyTestI";
    private static final String KEY_LTEST = "keyTestL";
    private static final String KEY_BTEST = "keyTestB";
    private static final String KEY_VALUE = "KeyValue";
    private static final int KEY_IVALUE = 1;
    private static final long KEY_LVALUE = 2L;
    private static final boolean KEY_BVALUE = true;

    @Test
    public final void testSystemPropertyDefault() {
        SystemPropertyUtil.refresh();
        assertTrue(SystemPropertyUtil.isFileEncodingCorrect());
        SystemPropertyUtil.set(SystemPropertyUtil.FILE_ENCODING, "UTF-16");
        assertTrue(SystemPropertyUtil.isFileEncodingCorrect());
        SystemPropertyUtil.set(SystemPropertyUtil.FILE_ENCODING, CharsetUtils.UTF_8);
        assertTrue(SystemPropertyUtil.isFileEncodingCorrect());
        SystemPropertyUtil.clear(SystemPropertyUtil.FILE_ENCODING);
        assertTrue(SystemPropertyUtil.isFileEncodingCorrect());
        SystemPropertyUtil.refresh();
        assertTrue(SystemPropertyUtil.isFileEncodingCorrect());
        final String config = VitamConfiguration.getVitamConfigFolder();
        assertNotNull(config);
        final String data = VitamConfiguration.getVitamDataFolder();
        assertNotNull(data);
        final String log = VitamConfiguration.getVitamLogFolder();
        assertNotNull(log);
        final String tmp = VitamConfiguration.getVitamTmpFolder();
        assertNotNull(tmp);
        SystemPropertyUtil.set(VitamConfiguration.VITAM_CONFIG_PROPERTY, KEY_VALUE);
        SystemPropertyUtil.set(VitamConfiguration.VITAM_DATA_PROPERTY, KEY_VALUE);
        SystemPropertyUtil.set(VitamConfiguration.VITAM_LOG_PROPERTY, KEY_VALUE);
        SystemPropertyUtil.set(VitamConfiguration.VITAM_TMP_PROPERTY, KEY_VALUE);
        VitamConfiguration.checkVitamConfiguration();
        assertFalse(VitamConfiguration.getVitamConfigFolder().equals(config));
        assertFalse(VitamConfiguration.getVitamDataFolder().equals(data));
        assertFalse(VitamConfiguration.getVitamLogFolder().equals(log));
        assertFalse(VitamConfiguration.getVitamTmpFolder().equals(tmp));
        SystemPropertyUtil.set(VitamConfiguration.VITAM_CONFIG_PROPERTY, config);
        SystemPropertyUtil.set(VitamConfiguration.VITAM_DATA_PROPERTY, data);
        SystemPropertyUtil.set(VitamConfiguration.VITAM_LOG_PROPERTY, log);
        SystemPropertyUtil.set(VitamConfiguration.VITAM_TMP_PROPERTY, tmp);
        VitamConfiguration.checkVitamConfiguration();
        assertTrue(VitamConfiguration.getVitamConfigFolder().equals(config));
        assertTrue(VitamConfiguration.getVitamDataFolder().equals(data));
        assertTrue(VitamConfiguration.getVitamLogFolder().equals(log));
        assertTrue(VitamConfiguration.getVitamTmpFolder().equals(tmp));
    }

    @Test
    public final void testSystemPropertyString() {
        SystemPropertyUtil.refresh();
        assertTrue(SystemPropertyUtil.isFileEncodingCorrect());
        SystemPropertyUtil.set(KEY_TEST, KEY_VALUE);
        assertTrue(SystemPropertyUtil.contains(KEY_TEST));
        assertEquals(KEY_VALUE, SystemPropertyUtil.get(KEY_TEST));
        assertEquals(KEY_VALUE, SystemPropertyUtil.get(KEY_TEST, OTHER));
        assertEquals(KEY_VALUE, SystemPropertyUtil.getAndSet(KEY_TEST, OTHER));
        assertEquals(OTHER, SystemPropertyUtil.getAndSet(KEY_TEST + "2", OTHER));
        assertEquals(OTHER, SystemPropertyUtil.set(KEY_TEST + "2", OTHER));
    }

    @Test
    public final void testSystemPropertyBoolean() {
        SystemPropertyUtil.set(KEY_BTEST, KEY_BVALUE);
        assertTrue(SystemPropertyUtil.contains(KEY_BTEST));
        assertEquals(Boolean.toString(KEY_BVALUE), SystemPropertyUtil.get(KEY_BTEST));
        assertEquals(KEY_BVALUE, SystemPropertyUtil.get(KEY_BTEST, false));
        assertEquals(KEY_BVALUE, SystemPropertyUtil.getAndSet(KEY_BTEST, false));
        assertEquals(false, SystemPropertyUtil.getAndSet(KEY_BTEST + "2", false));
        assertEquals(false, SystemPropertyUtil.set(KEY_BTEST + "2", false));
        assertEquals(false, SystemPropertyUtil.get(KEY_BTEST + "3", false));
        assertEquals(null, SystemPropertyUtil.set(KEY_BTEST + "3", "true"));
        assertEquals(true, SystemPropertyUtil.get(KEY_BTEST + "3", false));
        assertEquals("true", SystemPropertyUtil.set(KEY_BTEST + "3", "yes"));
        assertEquals(true, SystemPropertyUtil.get(KEY_BTEST + "3", false));
        assertEquals("yes", SystemPropertyUtil.set(KEY_BTEST + "3", "1"));
        assertEquals(true, SystemPropertyUtil.get(KEY_BTEST + "3", false));
        assertEquals("1", SystemPropertyUtil.set(KEY_BTEST + "3", "yes2"));
        assertEquals(false, SystemPropertyUtil.get(KEY_BTEST + "3", false));
        assertEquals("yes2", SystemPropertyUtil.set(KEY_BTEST + "3", ""));
        assertEquals(true, SystemPropertyUtil.get(KEY_BTEST + "3", false));
    }

    @Test
    public final void testSystemPropertyInt() {
        SystemPropertyUtil.set(KEY_ITEST, KEY_IVALUE);
        assertTrue(SystemPropertyUtil.contains(KEY_ITEST));
        assertEquals(Integer.toString(KEY_IVALUE), SystemPropertyUtil.get(KEY_ITEST));
        assertEquals(KEY_IVALUE, SystemPropertyUtil.get(KEY_ITEST, 4));
        assertEquals(KEY_IVALUE, SystemPropertyUtil.getAndSet(KEY_ITEST, 4));
        assertEquals(4, SystemPropertyUtil.getAndSet(KEY_ITEST + "2", 4));
        assertEquals(4, SystemPropertyUtil.set(KEY_ITEST + "2", 4));
        assertEquals(5, SystemPropertyUtil.get(KEY_ITEST + "3", 5));
        assertEquals(null, SystemPropertyUtil.set(KEY_ITEST + "3", "yes2"));
        assertEquals(6, SystemPropertyUtil.get(KEY_ITEST + "3", 6));
    }

    @Test
    public final void testSystemPropertyLong() {
        SystemPropertyUtil.set(KEY_LTEST, KEY_LVALUE);
        assertTrue(SystemPropertyUtil.contains(KEY_LTEST));
        assertEquals(Long.toString(KEY_LVALUE), SystemPropertyUtil.get(KEY_LTEST));
        assertEquals(KEY_LVALUE, SystemPropertyUtil.get(KEY_LTEST, 3L));
        assertEquals(KEY_LVALUE, SystemPropertyUtil.getAndSet(KEY_LTEST, 3L));
        assertEquals(3L, SystemPropertyUtil.getAndSet(KEY_LTEST + "2", 3L));
        assertEquals(3L, SystemPropertyUtil.set(KEY_LTEST + "2", 3L));
        assertEquals(4L, SystemPropertyUtil.get(KEY_LTEST + "3", 4L));
        assertEquals(null, SystemPropertyUtil.set(KEY_LTEST + "3", "yes2"));
        assertEquals(5L, SystemPropertyUtil.get(KEY_LTEST + "3", 5L));
    }

    @Test
    public final void testSystemPropertyDebug() {
        final AtomicBoolean bool = new AtomicBoolean(false);
        final OutputStream outputStream = new OutputStream() {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                bool.set(true);
            }

            @Override
            public void write(byte[] b) throws IOException {
                bool.set(true);
            }

            @Override
            public void write(int arg0) throws IOException {
                bool.set(true);
            }
        };
        final PrintStream out = new PrintStream(outputStream);
        SystemPropertyUtil.debug(out);
        assertTrue(bool.get());
    }

    @Test
    public final void testSystemPropertyOs() {
        SystemPropertyUtil.get(OS_NAME);
        final Platform platform = SystemPropertyUtil.getOS();
        switch (platform) {
            case MAC:
                assertTrue(SystemPropertyUtil.isMac());
                assertFalse(SystemPropertyUtil.isWindows());
                assertFalse(SystemPropertyUtil.isUnix());
                assertFalse(SystemPropertyUtil.isSolaris());
                break;
            case SOLARIS:
                assertFalse(SystemPropertyUtil.isMac());
                assertFalse(SystemPropertyUtil.isWindows());
                assertFalse(SystemPropertyUtil.isUnix());
                assertTrue(SystemPropertyUtil.isSolaris());
                break;
            case UNIX:
                assertFalse(SystemPropertyUtil.isMac());
                assertFalse(SystemPropertyUtil.isWindows());
                assertTrue(SystemPropertyUtil.isUnix());
                assertFalse(SystemPropertyUtil.isSolaris());
                break;
            case UNSUPPORTED:
                assertFalse(SystemPropertyUtil.isMac());
                assertFalse(SystemPropertyUtil.isWindows());
                assertFalse(SystemPropertyUtil.isUnix());
                assertFalse(SystemPropertyUtil.isSolaris());
                break;
            case WINDOWS:
                assertFalse(SystemPropertyUtil.isMac());
                assertTrue(SystemPropertyUtil.isWindows());
                assertFalse(SystemPropertyUtil.isUnix());
                assertFalse(SystemPropertyUtil.isSolaris());
                break;
            default:
                break;
        }
    }

    @Test
    public final void testSystemPropertyError() {
        try {
            SystemPropertyUtil.contains(null);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.get(null);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.get(null, KEY_IVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.get(null, KEY_BVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.get(null, KEY_LVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.getAndSet(null, KEY_VALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.getAndSet(null, KEY_BVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.getAndSet(null, KEY_IVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.getAndSet(null, KEY_LVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.set(null, KEY_VALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.set(null, KEY_BVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.set(null, KEY_IVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            SystemPropertyUtil.set(null, KEY_LVALUE);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
    }
}
