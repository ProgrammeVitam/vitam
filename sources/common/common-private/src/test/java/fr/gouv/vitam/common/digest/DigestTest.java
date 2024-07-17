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
package fr.gouv.vitam.common.digest;

import fr.gouv.vitam.common.ResourcesPrivateUtilTest;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DigestTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DigestTest.class);

    private static final String DGMD5 = "5c12f73ecc35d2f598a6cc8213040198";
    private static final String DGSHA1 = "5b5671a4d1e924c385fe5a169be5e94110754291";
    private static final String DGSHA256 = "0ba13cdab9714ec2fa589f0e495ca4c0d1c05910e06065b83dbb245e23d86856";
    private static final String DGSHA384 =
        "78a5338ab553bcaf72b2fdd50e793fb00fac5a532fea7083372690a44bbf1c8998839f12108e2be7397dfed264542623";
    private static final String DGSHA512 =
        "70242de950b2dce3b3f700505243c586c43249208d3aa463e00ac73d34f44a1bb5f1150ef6cb24f2859e2d33b45bfc5d59d67d92f54a9a43af478e37e0ae0fa9";

    @Test
    public void testDigestComplex() throws IOException {
        final byte[] byteArray0 = new byte[1];
        for (final DigestType digestType : DigestType.values()) {
            ByteArrayInputStream byteArrayInputStream0 = new ByteArrayInputStream(byteArray0);
            Digest digest0 = Digest.digest(byteArrayInputStream0, digestType);
            final String hex = digest0.digestHex();
            final byte[] byteArray1 = digest0.digest();
            final byte[] byteArray2 = digest0.digest();
            assertSame(byteArray1, byteArray2);
            assertTrue(digest0.equalsWithType(byteArray1, digestType));
            assertTrue(digest0.equalsWithType(hex, digestType));

            digest0 = new Digest(digestType);
            byteArrayInputStream0 = new ByteArrayInputStream(byteArray0);
            digest0.update(byteArrayInputStream0);
            digest0.reset();

            byteArrayInputStream0 = new ByteArrayInputStream(byteArray0);
            digest0.update(byteArrayInputStream0, 100);
            byteArrayInputStream0 = new ByteArrayInputStream(byteArray0);
            digest0.update(byteArrayInputStream0, 100, 0);
            assertNotNull(digest0.digestHex());
            assertFalse(digest0.digestHex().isEmpty());
            assertEquals(hex, digest0.digestHex());
            assertEquals(digestType, digest0.type());
            assertTrue(digest0.equalsWithType(byteArray1, digestType));
            assertTrue(digest0.equalsWithType(hex, digestType));
            assertFalse(digest0.equalsWithType(byteArray1, null));
            assertFalse(digest0.equalsWithType(hex, null));
            assertFalse(digest0.equalsWithType((String) null, digestType));
            assertFalse(digest0.equalsWithType((byte[]) null, digestType));
            assertEquals(hex, new Digest(hex, digestType).toString());
            assertEquals(hex.hashCode(), digest0.hashCode());

            digest0.reset();
            byteArrayInputStream0 = new ByteArrayInputStream(byteArray0);
            digest0.update(byteArrayInputStream0, 100, 2);
            assertEquals(hex, digest0.digestHex());

            digest0.reset();
            byteArrayInputStream0 = new ByteArrayInputStream(byteArray0);
            final InputStream inputStream = digest0.getDigestInputStream(byteArrayInputStream0);
            while (inputStream.read(byteArray1) >= 0) {}
            inputStream.close();
            assertEquals(hex, digest0.digestHex());
        }
    }

    @Test
    public void testDigestSimple() throws IOException {
        assertThatCode(() -> {
            final Digest digest = new Digest(DigestType.MD5);
            digest.update(new byte[2], 1, 0);
            digest.update((ByteBuffer) ByteBuffer.allocate(1).put((byte) 1).flip());
            digest.update(ByteBuffer.allocate(0));
            ByteBuffer bb = (ByteBuffer) ByteBuffer.wrap(new byte[4]).position(2);
            bb = bb.slice();
            digest.update(bb);
        }).doesNotThrowAnyException();
    }

    @Test
    public void testDigestSimpleFile() throws IOException {
        final File file = ResourcesPrivateUtilTest.getInstance().getGuidTestPropertiesFile();
        if (file == null) {
            LOGGER.error(ResourcesPrivateUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE);
        }
        Assume.assumeTrue(ResourcesPrivateUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE, file != null);

        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update(file);
            digest0.update(file, 0, 0);
            digest0.reset();
            digest0.update(file);
            final Digest digest1 = Digest.digest(file, DigestType.MD5);
            assertEquals(digest0, digest1);
        } catch (final IllegalArgumentException e) { // NOSONAR
            fail(ResourcesPrivateUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
    }

    @Test
    public void testSimpleTest() {
        final Digest digest = new Digest(DigestType.MD5);
        digest.update(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        assertEquals(DGMD5, digest.digestHex());
        assertTrue(digest.equals(DGMD5));
        assertTrue(digest.equals(digest.digest()));
        Digest digest2 = new Digest(DigestType.SHA1);
        digest2.update(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        assertEquals(DGSHA1, digest2.digestHex());
        assertTrue(digest2.equals(DGSHA1));
        assertTrue(digest2.equals(digest2.digest()));
        assertFalse(digest2.equals(digest));
        final Digest digest3 = new Digest(DigestType.SHA256);
        digest3.update(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        assertEquals(DGSHA256, digest3.digestHex());
        assertTrue(digest3.equals(DGSHA256));
        assertTrue(digest3.equals(digest3.digest()));
        assertFalse(digest3.equals(digest));
        final Digest digest4 = new Digest(DigestType.SHA384);
        digest4.update(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        assertEquals(DGSHA384, digest4.digestHex());
        assertTrue(digest4.equals(DGSHA384));
        assertTrue(digest4.equals(digest4.digest()));
        assertFalse(digest4.equals(digest));
        final Digest digest5 = new Digest(DigestType.SHA512);
        digest5.update(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        assertEquals(DGSHA512, digest5.digestHex());
        assertTrue(digest5.equals(DGSHA512));
        assertTrue(digest5.equals(digest5.digest()));
        assertFalse(digest5.equals(digest));
        digest2.reset();
        digest2 = new Digest(DigestType.MD5);
        digest2.reset();
        digest2.update(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        assertTrue(digest2.equals(digest));
        assertFalse(digest2.equals(null));
        assertFalse(digest2.equals(new Object()));

        final DigestType digestType = DigestType.fromValue(DigestType.SHA512.getName());
        assertEquals(DigestType.SHA512, digestType);
    }

    @Test
    public void testError() throws IOException {
        try {
            DigestType.fromValue("unknown");
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {
            // Ignore
        }
        try {
            Digest.digest((File) null, DigestType.MD5);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            Digest.digest((InputStream) null, DigestType.MD5);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update((byte[]) null);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update((ByteBuffer) null);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update((File) null);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update(new File("does not exist"));
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update((InputStream) null);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update((FileChannel) null);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update((String) null);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update(new byte[2], -1, 1);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update(new byte[2], 1, -1);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update(new byte[2], -1, -1);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update(new byte[2], 1, 2);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            final ByteArrayInputStream byteArrayInputStream0 = new ByteArrayInputStream(new byte[2]);
            digest0.update(byteArrayInputStream0, 0);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
    }

    @Test
    public void testErrorUsingFile() throws IOException {
        final File file = ResourcesPrivateUtilTest.getInstance().getGuidTestPropertiesFile();
        if (file == null) {
            LOGGER.error(ResourcesPrivateUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE);
        }
        Assume.assumeTrue(ResourcesPrivateUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE, file != null);

        try {
            final Digest digest0 = new Digest(DigestType.MD5);
            digest0.update(file, -1, 0);
            fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // Ignore
        }
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannel = inputStream.getChannel();
        try {
            try {
                final Digest digest0 = new Digest(DigestType.MD5);
                digest0.update(fileChannel, -1, 1, 0);
                fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
            } catch (final IllegalArgumentException e) { // NOSONAR
                // Ignore
            }
            try {
                final Digest digest0 = new Digest(DigestType.MD5);
                digest0.update(fileChannel, 0, 0, 0);
                fail(ResourcesPrivateUtilTest.SHOULD_HAVE_AN_EXCEPTION);
            } catch (final IllegalArgumentException e) { // NOSONAR
                // Ignore
            }
            try {
                final Digest digest0 = new Digest(DigestType.MD5);
                digest0.update(fileChannel);
            } catch (final IllegalArgumentException e) { // NOSONAR
                fail(ResourcesPrivateUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
            }
        } finally {
            fileChannel.close();
            inputStream.close();
        }
        inputStream = new FileInputStream(file);
        fileChannel = inputStream.getChannel();
        try {
            try {
                final Digest digest0 = new Digest(DigestType.MD5);
                digest0.update(fileChannel, 0, 1000000, 0);
                digest0.update(fileChannel, 0, 1000000, 10);
            } catch (final IllegalArgumentException e) { // NOSONAR
                fail(ResourcesPrivateUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
            }
        } finally {
            fileChannel.close();
            inputStream.close();
        }
    }
}
