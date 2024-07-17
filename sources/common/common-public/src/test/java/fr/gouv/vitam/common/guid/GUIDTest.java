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
package fr.gouv.vitam.common.guid;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ResourcesPublicUtilTest;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({ "javadoc" })
public class GUIDTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GUIDTest.class);

    private static final String WRONG_ARK3 = "ark:/1a/aeasppnwoyafrlybkt3kfuyaaaaac";

    private static final String WRONG_ARK2 = "ark:/1aeasppnwoyafrlybkt3kfuyaaaaac";

    private static final String WRONG_ARK1 = "ark:/1/aeasppnwoyafrlybkt3kfuyaaaaacaaaaa";

    private static final String WRONG_BYTES =
        "[2, 1, 0, 0, 0, 1, 39, -67, -74, 118, 0, 88, -81, 1, 84, -10, -94, -45, 0, 0, 0, 1]";
    private static final String WRONG_STRING_ID = "02010000000127bdb6760058af0154f6a2d300000001";

    private enum FIELDS {
        BASE16,
        BASE32,
        BASE64,
        BASEARK,
        COUNTER,
        OBJECTID,
        PLATFORMID,
        PROCESSID,
        TENANTID,
        TIMESTAMP,
        VERSION,
        HASHCODE,
        ARKNAME,
        MACFRAGMENT,
        BYTES,
        BASE16B,
        BASE32B,
        BASE64B,
        BASEARKB,
    }

    private static final int VERSION = 1 & 0x1F;
    private static final int HEXLENGTH = GUIDImpl.KEYSIZE * 2;
    private static Properties properties;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        final File file = ResourcesPublicUtilTest.getInstance().getGuidTestPropertiesFile();
        if (file == null) {
            LOGGER.error(ResourcesPublicUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE);
            properties = new Properties();
            properties.setProperty(FIELDS.BASE16.name(), "01010000000127bdb6760058af0154f6a2d300000001");
            properties.setProperty(FIELDS.BASE32.name(), "aeaqaaaaaet33ntwabmk6aku62rngaaaaaaq");
            properties.setProperty(FIELDS.BASE64.name(), "AQEAAAABJ722dgBYrwFU9qLTAAAAAQ");
            properties.setProperty(FIELDS.BASEARK.name(), "ark:/1/aeasppnwoyafrlybkt3kfuyaaaaac");
            properties.setProperty(
                FIELDS.BYTES.name(),
                "[1, 1, 0, 0, 0, 1, 39, -67, -74, 118, 0, 88, -81, 1, 84, -10, -94, -45, 0, 0, 0, 1]"
            );
        } else {
            properties = PropertiesUtils.readProperties(file);
        }
    }

    @Test
    public void testStructure() {
        GUIDImpl id;
        try {
            id = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
            final String str = id.toHex();

            assertEquals('0', str.charAt(0));
            assertEquals('1', str.charAt(1));
            assertEquals(HEXLENGTH, str.length());
            LOGGER.debug(id.toArk() + " = " + id.toString());
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(e);
            fail("Should not raize an exception");
        }
    }

    @Test
    public void testParsing() {
        for (int i = 0; i < 1000; i++) {
            GUIDImpl id1;
            try {
                id1 = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
            } catch (final InvalidGuidOperationException e) {
                LOGGER.debug(e);
                fail("Should not raize an exception");
                return;
            }
            GUIDImpl id2;
            try {
                id2 = new GUIDImpl(id1.toHex());
                assertEquals(id1, id2);
                assertEquals(id1.hashCode(), id2.hashCode());
                assertEquals(0, id1.compareTo(id2));

                final GUIDImpl id3 = new GUIDImpl(id1.getBytes());
                assertEquals(id1, id3);
                assertEquals(id1.hashCode(), id3.hashCode());
                assertEquals(0, id1.compareTo(id3));

                final GUIDImpl id4 = new GUIDImpl(id1.toBase32());
                assertEquals(id1, id4);
                assertEquals(id1.hashCode(), id4.hashCode());
                assertEquals(0, id1.compareTo(id4));

                final GUIDImpl id5 = new GUIDImpl(id1.toArk());
                assertEquals(id1, id5);
                assertEquals(id1.hashCode(), id5.hashCode());
                assertEquals(0, id1.compareTo(id5));
            } catch (final InvalidGuidOperationException e) {
                LOGGER.debug(e);
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testGetBytesImmutability() {
        GUIDImpl id;
        try {
            id = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(e);
            fail("Should not raize an exception");
            return;
        }
        final byte[] bytes = id.getBytes();
        final byte[] original = Arrays.copyOf(bytes, bytes.length);
        bytes[0] = 0;
        bytes[1] = 0;
        bytes[2] = 0;

        assertTrue(Arrays.equals(id.getBytes(), original));
    }

    @Test
    public void testVersionField() {
        try {
            final GUIDImpl parsed1 = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
            assertEquals(VERSION, parsed1.getVersion());
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testHexBase32() {
        try {
            final GUIDImpl parsed1 = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
            final GUIDImpl parsed2 = new GUIDImpl(properties.getProperty(FIELDS.BASE64.name()));
            final GUIDImpl parsed0 = new GUIDImpl(properties.getProperty(FIELDS.BASE16.name()));
            final GUIDImpl parsed8 = new GUIDImpl(properties.getProperty(FIELDS.BASEARK.name()));
            final byte[] bytes = StringUtils.getBytesFromArraysToString(properties.getProperty(FIELDS.BYTES.name()));
            final GUIDImpl parsed9 = new GUIDImpl(bytes);
            assertTrue(parsed1.equals(parsed2));
            assertTrue(parsed1.equals(parsed0));
            assertTrue(parsed1.equals(parsed8));
            assertTrue(parsed1.equals(parsed9));
            final GUIDImpl parsed3 = new GUIDImpl(parsed9.getBytes());
            final GUIDImpl parsed4 = new GUIDImpl(parsed9.toBase32());
            final GUIDImpl parsed5 = new GUIDImpl(parsed9.toHex());
            final GUIDImpl parsed6 = new GUIDImpl(parsed9.toString());
            final GUIDImpl parsed7 = new GUIDImpl(parsed9.toBase64());
            assertTrue(parsed9.equals(parsed3));
            assertTrue(parsed9.equals(parsed4));
            assertTrue(parsed9.equals(parsed5));
            assertTrue(parsed9.equals(parsed6));
            assertTrue(parsed9.equals(parsed7));
            final GUIDImpl generated = new GUIDImpl();
            assertTrue(generated.getVersion() == 0);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testJson() throws InvalidGuidOperationException {
        GUIDImpl guid;
        try {
            guid = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(e);
            fail("Should not raize an exception");
            return;
        }
        LOGGER.debug("HEX:" + guid.toHex());
        LOGGER.debug("BASE32: " + guid.toBase32());
        LOGGER.debug("BASE64: " + guid.toBase64());
        try {
            final String json = JsonHandler.writeAsString(guid);
            LOGGER.debug(json);
            final GUIDImpl uuid2 = JsonHandler.getFromString(json, GUIDImpl.class);
            assertEquals("Json check", guid, uuid2);
            final GUID guid2 = GUIDReader.getGUID(guid.getId());
            final String json2 = JsonHandler.writeAsString(guid2);
            LOGGER.debug(json2);
            final GUIDImpl uuid3 = JsonHandler.getFromString(json, GUIDImpl.class);
            assertEquals("Json check", guid, uuid3);
        } catch (final InvalidParseOperationException e) {
            LOGGER.debug(e);
            fail("Exception occurs: " + e.getMessage());
            return;
        }
    }

    @Test
    public final void testIllegalArgument() {
        try {
            new GUIDImpl(WRONG_ARK1);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION, e);
        }
        try {
            new GUIDImpl(WRONG_ARK2);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION, e);
        }
        try {
            new GUIDImpl(WRONG_ARK3);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION, e);
        }
        final byte[] bytes = StringUtils.getBytesFromArraysToString(WRONG_BYTES);
        try {
            new GUIDImpl(bytes);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION, e);
        }
        try {
            new GUIDImpl(WRONG_STRING_ID);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION, e);
        }
        GUIDImpl guid = null;
        GUIDImpl guid2 = null;
        try {
            guid = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
            guid2 = new GUIDImpl(properties.getProperty(FIELDS.BASE16.name()));
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION, e);
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
            return;
        }
        assertFalse(guid.equals(null));
        assertFalse(guid.equals(new Object()));
        assertTrue(guid.equals(guid));
        assertTrue(guid.equals(guid2));
    }
}
