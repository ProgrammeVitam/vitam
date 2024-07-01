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

package fr.gouv.vitam.common.digest;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Digest implementation
 */
public class Digest {

    private static final String IGNORE = "Ignore";

    private static final String ARGUMENT_MUST_NOT_BE_NULL = "Argument must not be null";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Digest.class);

    private static final int BUFFER_SIZE = 8192;
    MessageDigest messageDigest;
    DigestType type;
    volatile byte[] finalized = null;
    private byte[] reusableBytes = null;

    /**
     * Create one DigestLight
     *
     * @param algo the algorithm to use
     * @throws IllegalArgumentException if null or unknown algorithm
     */
    public Digest(DigestType algo) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, algo);
        try {
            messageDigest = MessageDigest.getInstance(algo.getName());
            type = algo;
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Algo unknown", e);
        }
    }

    /**
     * Create one DigestLight from parameter
     *
     * @param digest as String to create
     * @param algo the algorithm to use
     * @throws IllegalArgumentException if null or unknown algorithm or if digest is null or empty
     */
    public Digest(String digest, DigestType algo) {
        this(algo);
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, digest);
        finalized = BaseXx.getFromBase16(digest);
    }

    /**
     * @return the associated digest type
     */
    public final DigestType type() {
        return type;
    }

    /**
     * @param bytes the bytes from which to update
     * @return this
     * @throws IllegalArgumentException if bytes null
     */
    public final Digest update(byte[] bytes) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, bytes);
        return update(bytes, 0, bytes.length);
    }

    /**
     * @param bytes the bytes from which to update
     * @param offset the offset position
     * @param length the length
     * @return this
     * @throws IllegalArgumentException if bytes null, offset &lt; 0, length &lt; 0
     */
    public final Digest update(byte[] bytes, int offset, int length) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, bytes);
        ParametersChecker.checkValue("offset", offset, 0);
        ParametersChecker.checkValue("length", length, 0);
        if (offset + length > bytes.length) {
            throw new IllegalArgumentException(
                "Range is incorrect: " + offset + ":" + length + " while length is " + bytes.length
            );
        }
        if (length == 0) {
            return this;
        }
        finalized = null;
        messageDigest.update(bytes, offset, length);
        return this;
    }

    /**
     * @param buffer for updating Digest
     * @return this
     * @throws IllegalArgumentException if buffer null
     */
    public final Digest update(ByteBuffer buffer) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, buffer);
        final int length = buffer.remaining();
        if (length == 0) {
            return this;
        }
        byte[] newbuf;
        int start = 0;
        if (buffer.hasArray()) {
            start = buffer.arrayOffset();
            newbuf = buffer.array();
        } else {
            newbuf = new byte[length];
            buffer.get(newbuf);
        }
        return update(newbuf, start, length);
    }

    /**
     * @param value the String value from which to update
     * @return this
     * @throws IllegalArgumentException value null
     */
    public final Digest update(String value) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, value);
        return update(value.getBytes(CharsetUtils.UTF8));
    }

    /**
     * @param in the file from which to update
     * @return this
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException in null
     */
    public final Digest update(File in) throws IOException {
        return update(in, 0, -1);
    }

    /**
     * @param in the file from which to update
     * @param start the position to start
     * @param limit if less than 0, means all
     * @return this
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException in null, start &lt; 0
     */
    public final Digest update(File in, long start, long limit) throws IOException {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, in);

        if (!in.isFile()) {
            throw new IllegalArgumentException("File not found");
        }
        ParametersChecker.checkValue("start", start, 0);
        if (limit == 0) {
            return this;
        }
        try (FileInputStream inputStream = new FileInputStream(in)) {
            try (FileChannel fcin = inputStream.getChannel()) {
                return update(fcin, start, BUFFER_SIZE, limit);
            }
        }
    }

    /**
     * @param inputStream the inputstream from which to update using default chunksize
     * @return this
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException inputstream null
     */
    public final Digest update(InputStream inputStream) throws IOException {
        return update(inputStream, BUFFER_SIZE, -1);
    }

    /**
     * @param inputStream the inputstream from which to update
     * @param chunkSize the chunksize to use
     * @return this
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException inputstream null, chunksize &lt; 1
     */
    public final Digest update(InputStream inputStream, int chunkSize) throws IOException {
        return update(inputStream, chunkSize, -1);
    }

    /**
     * @param inputStream the inputstream from which to update
     * @param chunkSize the chunksize to use
     * @param limit if less than 0, means all
     * @return this
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException inputstream null, chunksize &lt; 1
     */
    public final Digest update(InputStream inputStream, int chunkSize, long limit) throws IOException {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, inputStream);
        ParametersChecker.checkValue("chunkSize", chunkSize, 1);

        if (limit == 0) {
            return this;
        }
        finalized = null;
        final byte[] buf = setReusableByte(chunkSize);
        try {
            int size;
            while ((size = inputStream.read(buf)) >= 0) {
                messageDigest.update(buf, 0, size);
            }
            return this;
        } finally {
            try {
                inputStream.close();
            } catch (final Exception e) {
                LOGGER.debug(IGNORE, e);
                // ignore
            }
        }
    }

    /**
     * @param fileChannelInputStream the FileChannel inputstream from which to update
     * @return this
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException fileChannelIinputStream null
     */
    public final Digest update(FileChannel fileChannelInputStream) throws IOException {
        return update(fileChannelInputStream, 0, BUFFER_SIZE, -1);
    }

    /**
     * @param fileChannelInputStream the FileChannel inputstream from which to update
     * @param start the position to start
     * @param chunkSize the chunksize to use
     * @param limit if less than 0, means all
     * @return this
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException fileChannelIinputStream null, start &lt; 0, chunksize &lt; 1
     */
    public final Digest update(FileChannel fileChannelInputStream, long start, int chunkSize, long limit)
        throws IOException {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, fileChannelInputStream);
        ParametersChecker.checkValue("chunkSize", chunkSize, 1);
        ParametersChecker.checkValue("start", start, 0);
        if (limit == 0) {
            return this;
        }
        finalized = null;
        final byte[] buf = setReusableByte(chunkSize);
        try {
            final ByteBuffer bb = ByteBuffer.wrap(buf);
            if (start > 0) {
                fileChannelInputStream.position(start);
            }
            int size;
            while ((size = fileChannelInputStream.read(bb)) >= 0) {
                messageDigest.update(buf, 0, size);
                bb.clear();
            }
            return this;
        } finally {
            try {
                fileChannelInputStream.close();
            } catch (final Exception e) {
                LOGGER.debug(IGNORE, e);
                // ignore
            }
        }
    }

    /**
     * Will update the Digest while the returned InputStream will be read
     *
     * @param inputStream from which the data to digest will be done
     * @return the new InputStream to use instead of the given one as parameter
     */
    public InputStream getDigestInputStream(InputStream inputStream) {
        return new DigestInputStream(inputStream, messageDigest);
    }

    /**
     * Will update the Digest while the returned OutputStream will be read
     *
     * @param outputStream to which the data to digest will be written
     * @return the new OutputStream to use instead of the given one as parameter
     */
    public OutputStream getDigestOutputStream(OutputStream outputStream) {
        return new DigestOutputStream(outputStream, messageDigest);
    }

    /**
     * Reset the DigestLight
     *
     * @return this
     */
    public final Digest reset() {
        messageDigest.reset();
        finalized = null;
        return this;
    }

    /**
     * @return the digest
     */
    public final byte[] digest() {
        if (finalized == null) {
            MessageDigest dclone;
            try {
                dclone = (MessageDigest) messageDigest.clone();
                finalized = dclone.digest();
            } catch (final CloneNotSupportedException e) {
                LOGGER.debug(IGNORE, e);
                // ignore
                finalized = messageDigest.digest();
            }
        }
        return finalized;
    }

    /**
     * @return the digest in Base16 format
     */
    public final String digestHex() {
        if (finalized == null) {
            MessageDigest dclone;
            try {
                dclone = (MessageDigest) messageDigest.clone();
                finalized = dclone.digest();
            } catch (final CloneNotSupportedException e) {
                LOGGER.debug(IGNORE, e);
                // ignore
                finalized = messageDigest.digest();
            }
        }
        return BaseXx.getBase16(finalized);
    }

    /**
     * @return the digest in Base64 format
     */
    public final String digest64() {
        return BaseXx.getBase64(digest());
    }

    @Override
    public final String toString() {
        return digestHex();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Digest) {
            final Digest digest = (Digest) obj;
            return digest.type == type && MessageDigest.isEqual(digest(), digest.digest());
        }
        if (obj instanceof String) {
            return ((String) obj).equalsIgnoreCase(digestHex());
        }
        if (obj instanceof byte[]) {
            return MessageDigest.isEqual(digest(), (byte[]) obj);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return digestHex().hashCode();
    }

    /**
     * @param digest the digest to compare to
     * @param algo the associated algorithm
     * @return True if the 2 digests are of the same type and same value
     */
    public final boolean equalsWithType(String digest, DigestType algo) {
        if (digest == null || algo == null) {
            return false;
        }
        return algo == type && digest.equalsIgnoreCase(digestHex());
    }

    /**
     * @param digest the digest in byte to use
     * @param algo the associated algorithm
     * @return True if the 2 digests are of the same type and same value
     */
    public final boolean equalsWithType(byte[] digest, DigestType algo) {
        if (digest == null || algo == null) {
            return false;
        }
        return algo == type && MessageDigest.isEqual(digest(), digest);
    }

    private byte[] setReusableByte(int length) {
        if (reusableBytes == null || reusableBytes.length != length) {
            reusableBytes = new byte[length];
        }
        return reusableBytes;
    }

    /**
     * @param in the inputstream from which the digest will be computed
     * @param algo the algorithm to use
     * @return the digest for this inputStream
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException in or algo null
     */
    public static Digest digest(InputStream in, DigestType algo) throws IOException {
        return new Digest(algo).update(in);
    }

    /**
     * @param in the file from which the digest will be computed
     * @param algo the algorithm to use
     * @return the digest for this File
     * @throws IOException if any IO error occurs
     * @throws IllegalArgumentException in or algo null
     */
    public static Digest digest(File in, DigestType algo) throws IOException {
        return new Digest(algo).update(in);
    }
}
