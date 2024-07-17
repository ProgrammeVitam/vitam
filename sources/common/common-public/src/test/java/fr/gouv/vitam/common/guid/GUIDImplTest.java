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

/**
 *
 */
public class GUIDImplTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GUIDImplTest.class);

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

    private static Properties properties;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        final File file = ResourcesPublicUtilTest.getInstance().getGuidTestPropertiesFile();
        if (file == null) {
            LOGGER.error("CANNOT FIND RESOURCES TEST FILE");
            properties = new Properties();
            properties.setProperty(FIELDS.BASE16.name(), "01010000000127bdb6760058af0154f6a2d300000001");
            properties.setProperty(FIELDS.BASE32.name(), "aeaqaaaaaet33ntwabmk6aku62rngaaaaaaq");
            properties.setProperty(FIELDS.BASE64.name(), "AQEAAAABJ722dgBYrwFU9qLTAAAAAQ");
            properties.setProperty(FIELDS.BASEARK.name(), "ark:/1/aeasppnwoyafrlybkt3kfuyaaaaac");
            properties.setProperty(FIELDS.COUNTER.name(), "1");
            properties.setProperty(FIELDS.OBJECTID.name(), "1");
            properties.setProperty(FIELDS.PLATFORMID.name(), "666744438");
            properties.setProperty(FIELDS.PROCESSID.name(), "22703");
            properties.setProperty(FIELDS.TENANTID.name(), "1");
            properties.setProperty(FIELDS.TIMESTAMP.name(), "1464426746624");
            properties.setProperty(FIELDS.VERSION.name(), "1");
            properties.setProperty(FIELDS.HASHCODE.name(), "-2034847754");
            properties.setProperty(FIELDS.ARKNAME.name(), "aeasppnwoyafrlybkt3kfuyaaaaac");
            properties.setProperty(FIELDS.MACFRAGMENT.name(), "[0, 0, 39, -67, -74, 118]");
            properties.setProperty(
                FIELDS.BYTES.name(),
                "[1, 1, 0, 0, 0, 1, 39, -67, -74, 118, 0, 88, -81, 1, 84, -10, -94, -45, 0, 0, 0, 1]"
            );
            properties.setProperty(FIELDS.BASE16B.name(), "010100000000a7bdb6760058af0154f6a2d368000001");
            properties.setProperty(FIELDS.BASE32B.name(), "aeaqaaaaact33ntwabmk6aku62rng2aaaaaq");
            properties.setProperty(FIELDS.BASE64B.name(), "AQEAAAAAp722dgBYrwFU9qLTaAAAAQ");
            properties.setProperty(FIELDS.BASEARKB.name(), "ark:/0/aea2ppnwoyafrlybkt3kfu3iaaaac");
        } else {
            properties = PropertiesUtils.readProperties(file);
        }
    }

    /**
     * Test method for {@link fr.gouv.vitam.common.guid.GUIDImpl#hashCode()}.
     */
    @Test
    public final void testInternalCodes() {
        assertEquals(GUIDImpl.KEYSIZE, GUIDImpl.getKeySize());
        GUIDImpl guid = null;
        try {
            guid = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION, e);
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
            return;
        }
        assertEquals(Integer.parseInt(properties.getProperty(FIELDS.COUNTER.name())), guid.getCounter());
        assertEquals(Integer.parseInt(properties.getProperty(FIELDS.OBJECTID.name())), guid.getObjectId());
        assertEquals(Integer.parseInt(properties.getProperty(FIELDS.PROCESSID.name())), guid.getProcessId());
        assertEquals(Integer.parseInt(properties.getProperty(FIELDS.PLATFORMID.name())), guid.getPlatformId());
        assertEquals(Integer.parseInt(properties.getProperty(FIELDS.TENANTID.name())), guid.getTenantId());
        assertEquals(Long.parseLong(properties.getProperty(FIELDS.TIMESTAMP.name())), guid.getTimestamp());
        assertEquals(Integer.parseInt(properties.getProperty(FIELDS.VERSION.name())), guid.getVersion());
        assertEquals(Integer.parseInt(properties.getProperty(FIELDS.HASHCODE.name())), guid.hashCode());
        assertEquals(properties.getProperty(FIELDS.ARKNAME.name()), guid.toArkName());
        byte[] bytes = StringUtils.getBytesFromArraysToString(properties.getProperty(FIELDS.MACFRAGMENT.name()));
        assertTrue(Arrays.equals(bytes, guid.getMacFragment()));
        bytes = StringUtils.getBytesFromArraysToString(properties.getProperty(FIELDS.BYTES.name()));
        assertTrue(Arrays.equals(bytes, guid.getBytes()));
    }

    /**
     * Test method for {@link fr.gouv.vitam.common.guid.GUIDImpl#equals(java.lang.Object)}.
     */
    @Test
    public final void testEqualsObject() {
        try {
            final GUIDImpl parsed1 = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
            final GUIDImpl parsed2 = new GUIDImpl(properties.getProperty(FIELDS.BASE64.name()));
            final GUIDImpl parsed3 = new GUIDImpl(properties.getProperty(FIELDS.BASE16.name()));
            final GUIDImpl parsed4 = new GUIDImpl(properties.getProperty(FIELDS.BASEARK.name()));
            final byte[] bytes = StringUtils.getBytesFromArraysToString(properties.getProperty(FIELDS.BYTES.name()));
            final GUIDImpl parsed5 = new GUIDImpl(bytes);
            assertTrue(parsed1.equals(parsed2));
            assertTrue(parsed1.equals(parsed3));
            assertTrue(parsed1.equals(parsed4));
            assertTrue(parsed1.equals(parsed5));
            assertEquals(properties.getProperty(FIELDS.BASE32.name()), parsed1.toString());
            assertEquals(properties.getProperty(FIELDS.BASE32.name()), parsed1.getId());
            assertEquals(properties.getProperty(FIELDS.BASE32.name()), parsed1.toBase32());
            assertEquals(properties.getProperty(FIELDS.BASE64.name()), parsed1.toBase64());
            assertEquals(properties.getProperty(FIELDS.BASE16.name()), parsed1.toHex());
            assertEquals(properties.getProperty(FIELDS.BASEARK.name()), parsed1.toArk());
            assertEquals(properties.getProperty(FIELDS.ARKNAME.name()), parsed1.toArkName());
        } catch (final InvalidGuidOperationException e) {
            LOGGER.debug(e);
            fail(e.getMessage());
        }
    }

    /**
     * Test method for {@link fr.gouv.vitam.common.guid.GUIDImpl#isWorm()}.
     */
    @Test
    public final void testIsWorm() {
        GUIDImpl guid = null;
        try {
            guid = new GUIDImpl(properties.getProperty(FIELDS.BASE32.name()));
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION, e);
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        assertFalse(guid.isWorm());
        try {
            guid = new GUIDImpl(properties.getProperty(FIELDS.BASE32B.name()));
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION, e);
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        assertTrue(guid.isWorm());
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
