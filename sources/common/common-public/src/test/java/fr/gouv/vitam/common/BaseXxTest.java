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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BaseXxTest {

    @Test(expected = IllegalArgumentException.class)
    public void testBase16() throws IOException {
        BaseXx.getBase16(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBase32() throws FileNotFoundException {
        BaseXx.getBase32(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBase64() throws IOException {
        BaseXx.getBase64UrlWithoutPadding(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBase64UrlPadding() throws IOException {
        BaseXx.getBase64UrlWithPadding(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBase16() throws IOException {
        BaseXx.getFromBase16(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBase32() throws FileNotFoundException {
        BaseXx.getFromBase32(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBase64() throws IOException {
        BaseXx.getFromBase64UrlWithoutPadding(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBase64Padding() throws IOException {
        BaseXx.getFromBase64UrlPadding(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
    }

    @Test
    public void testBase64UrlPaddingOK() throws IOException {
        final String encoded = BaseXx.getBase64UrlWithPadding("VitamTest64P".getBytes());
        assertNotNull(encoded);
        final byte[] bytes = BaseXx.getFromBase64UrlPadding(encoded);
        assertNotNull(bytes);
        assertTrue(Arrays.equals(bytes, "VitamTest64P".getBytes()));
    }

    @Test
    public void testBase64PaddingOK() throws IOException {
        final String encoded = BaseXx.getBase64("VitamTest64P".getBytes());
        assertNotNull(encoded);
        final byte[] bytes = BaseXx.getFromBase64(encoded);
        assertNotNull(bytes);
        assertTrue(Arrays.equals(bytes, "VitamTest64P".getBytes()));
    }

    @Test
    public void testBase64UrlWithoutPaddingOK() throws IOException {
        final String encoded = BaseXx.getBase64UrlWithoutPadding("VitamTest64".getBytes());
        assertNotNull(encoded);
        final byte[] bytes = BaseXx.getFromBase64UrlWithoutPadding(encoded);
        assertNotNull(bytes);
        assertTrue(Arrays.equals(bytes, "VitamTest64".getBytes()));
    }

    @Test
    public void testBase32OK() throws IOException {
        final String encoded = BaseXx.getBase32("VitamTest32".getBytes());
        assertNotNull(encoded);
        final byte[] bytes = BaseXx.getFromBase32(encoded);
        assertNotNull(bytes);
        assertTrue(Arrays.equals(bytes, "VitamTest32".getBytes()));
    }

    @Test
    public void testBase16OK() throws IOException {
        final String encoded = BaseXx.getBase16("VitamTest16".getBytes());
        assertNotNull(encoded);
        final byte[] bytes = BaseXx.getFromBase16(encoded);
        assertNotNull(bytes);
        assertTrue(Arrays.equals(bytes, "VitamTest16".getBytes()));
    }
}
