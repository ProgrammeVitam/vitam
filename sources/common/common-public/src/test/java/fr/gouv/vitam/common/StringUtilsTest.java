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

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
        assertArrayEquals(new byte[] { (byte) 7 }, byteArray0);
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
}
