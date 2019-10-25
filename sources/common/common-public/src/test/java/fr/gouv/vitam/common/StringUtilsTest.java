/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class StringUtilsTest {
    @Test
    public void testRandom() {
        final byte[] byteArray0 = StringUtils.getRandom(90);
        assertNotNull(byteArray0);
        final byte[] byteArray1 = StringUtils.getRandom(90);
        assertFalse(Arrays.equals(byteArray0, byteArray1));
        final byte[] byteArray2 = StringUtils.getBytesFromArraysToString(", ");
        assertArrayEquals(new byte[] {}, byteArray2);
        final byte[] byteArray3 = StringUtils.getRandom(0);
        assertArrayEquals(new byte[] {}, byteArray3);
        final byte[] byteArray4 = StringUtils.getRandom(-10);
        assertArrayEquals(new byte[] {}, byteArray4);
    }

    @Test
    public void testBytesFromArrayToString() {
        final byte[] byteArray0 = StringUtils.getBytesFromArraysToString("7");
        assertArrayEquals(new byte[] {(byte) 7}, byteArray0);
        final byte[] byteArray1 = StringUtils.getRandom(90);
        final String sbyte = Arrays.toString(byteArray1);
        final byte[] byteArray2 = StringUtils.getBytesFromArraysToString(sbyte);
        assertArrayEquals(byteArray1, byteArray2);

    }

    @Test
    public void testException() {
        try {
            StringUtils.getBytesFromArraysToString((String) null);
            fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            StringUtils.getBytesFromArraysToString("");
            fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            StringUtils.getBytesFromArraysToString("[ 7, a6 ]");
            fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
    }

    @Test
    public void testSingletons() throws IOException {
        final byte[] bytes = SingletonUtils.getSingletonByteArray();
        assertEquals(0, bytes.length);

        final List<StringUtilsTest> emptyList = SingletonUtils.singletonList();
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
        try {
            emptyList.add(this);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final UnsupportedOperationException e) {// NOSONAR
            // ignore
        }
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
        try {
            emptyList.remove(0);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final UnsupportedOperationException e) {// NOSONAR
            // ignore
        }
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());

        final Set<StringUtilsTest> emptySet = SingletonUtils.singletonSet();
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
        try {
            emptySet.add(this);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final UnsupportedOperationException e) {// NOSONAR
            // ignore
        }
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
        emptySet.remove(this);
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());

        final Map<StringUtilsTest, StringUtilsTest> emptyMap = SingletonUtils.singletonMap();
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyMap.size());
        try {
            emptyMap.put(this, this);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final UnsupportedOperationException e) {// NOSONAR
            // ignore
        }
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyMap.size());
        emptyMap.remove(this);
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyMap.size());

        final Iterator<StringUtilsTest> emptyIterator = SingletonUtils.singletonIterator();
        assertFalse(emptyIterator.hasNext());

        final InputStream emptyIS = SingletonUtils.singletonInputStream();
        final byte[] buffer = new byte[10];
        assertEquals(0, emptyIS.available());
        assertEquals(0, emptyIS.skip(10));
        assertEquals(-1, emptyIS.read());
        assertEquals(-1, emptyIS.read(buffer));
        assertEquals(-1, emptyIS.read(buffer, 0, buffer.length));
        assertEquals(true, emptyIS.markSupported());
        emptyIS.mark(5);
        emptyIS.reset();
        emptyIS.close();

        // No error
        final OutputStream voidOS = SingletonUtils.singletonOutputStream();
        voidOS.write(buffer);
        voidOS.write(1);
        voidOS.write(buffer, 0, buffer.length);
        voidOS.flush();
        voidOS.close();

    }
}
