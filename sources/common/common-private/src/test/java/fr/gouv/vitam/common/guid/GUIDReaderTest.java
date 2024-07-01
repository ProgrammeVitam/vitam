/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.guid;

import fr.gouv.vitam.common.ResourcesPublicUtilTest;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GUIDReaderTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GUIDReaderTest.class);
    private static final String BASE16 = "01010000000127bdb6760056700154f67df7e6000001";
    private static final String BASE32 = "aeaqaaaaaet33ntwablhaaku6z67pzqaaaaq";
    private static final String BASE64 = "AQEAAAABJ722dgBWcAFU9n335gAAAQ";
    private static final String ARK = "ark:/1/aeasppnwoyafm4abkt3h357gaaaac";

    @Test
    public final void testGetGUIDString() {
        try {
            final GUID guid16 = GUIDReader.getGUID(BASE16);
            final GUID guid32 = GUIDReader.getGUID(BASE32);
            final GUID guid64 = GUIDReader.getGUID(BASE64);
            final GUID guidArk = GUIDReader.getGUID(ARK);
            assertEquals(guid32, guid64);
            assertEquals(guid32, guid16);
            assertEquals(guid16, guid64);
            assertEquals(guid32, guidArk);
            assertEquals(guid16, guidArk);
            assertEquals(guid64, guidArk);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION, e);
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try {
            GUIDReader.getGUID((String) null);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) { // NOSONAR
            // Ignore
        }
        try {
            GUIDReader.getGUID("");
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) { // NOSONAR
            // Ignore
        }
        try {
            GUIDReader.getGUID("aa");
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) { // NOSONAR
            // Ignore
        }
        try {
            GUIDReader.getGUID("Attempted to parse malformed GUID: (");
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) { // NOSONAR
            // Ignore
        }
    }

    @Test
    public final void testGetGUIDByteArray() {
        byte[] baguid = null;
        GUID guid = null;
        try {
            guid = GUIDReader.getGUID(BASE32);
            baguid = guid.getBytes();
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION, e);
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try {
            final GUID bguid = GUIDReader.getGUID(baguid);
            assertEquals(bguid, guid);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION, e);
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try {
            GUIDReader.getGUID((byte[]) null);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) { // NOSONAR
            // Ignore
        }
        try {
            GUIDReader.getGUID(new byte[0]);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) { // NOSONAR
            // Ignore
        }
        try {
            final byte[] ba = { 1, 2 };
            GUIDReader.getGUID(ba);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final InvalidGuidOperationException e) { // NOSONAR
            // Ignore
        }
    }
}
