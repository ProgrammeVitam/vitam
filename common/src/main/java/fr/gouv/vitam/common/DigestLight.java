/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/

package fr.gouv.vitam.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * DigestLight implementation
 */
public class DigestLight {

    MessageDigest digest;
    DigestTypeLight type;
    volatile byte[] finalized = null;

    /**
     * Create one DigestLight
     * 
     * @param algo
     *            the algorithm to use
     */
    public DigestLight(DigestTypeLight algo) {
        try {
            digest = MessageDigest.getInstance(algo.name);
            type = algo;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Algo unknown", e);
        }
    }

    /**
     * Create one DigestLight from parameter
     * 
     * @param digest
     * @param algo
     *            the algorithm to use
     */
    public DigestLight(String digest, DigestTypeLight algo) {
        this(algo);
        finalized = BaseXx.getFromBase16(digest);
    }

    /**
     * 
     * @return the associated digest type
     */
    public DigestTypeLight type() {
        return type;
    }

    /**
     * 
     * @param bytes
     *            the bytes from which to update
     * @param offset
     *            the offset position
     * @param length
     *            the length
     * @return this
     */
    public DigestLight update(byte[] bytes, int offset, int length) {
        if (length == 0) {
            return this;
        }
        finalized = null;
        digest.update(bytes, offset, length);
        return this;
    }

    /**
     * 
     * @param bytes
     *            the bytes from which to update
     * @return this
     */
    public DigestLight update(byte[] bytes) {
        return update(bytes, 0, bytes.length);
    }

    /**
     * 
     * @param value
     *            the String value from which to update
     * @return this
     */
    public DigestLight update(String value) {
        return update(value.getBytes(FileUtil.UTF8));
    }

    /**
     * @param in
     *            the file from which to update
     * @param start
     *            the position to start
     * @param limit
     *            if less than 0, means all
     * @return this
     * @throws IOException
     *             if any IO error occurs
     */
    public DigestLight update(File in, long start, long limit) throws IOException {
        if (limit == 0) {
            return this;
        }
        finalized = null;
        int size = 0;
        byte[] buf = new byte[65536];
        FileInputStream inputStream = null;
        FileChannel fcin = null;
        limit = limit > 0 ? limit : Long.MAX_VALUE;
        long read = 0;
        try {
            inputStream = new FileInputStream(in);
            fcin = inputStream.getChannel();
            ByteBuffer bb = ByteBuffer.wrap(buf);
            if (start > 0) {
                fcin.position(start);
            }
            while ((size = fcin.read(bb)) >= 0) {
                if (read + size > limit) {
                    size = (int) (limit - read);
                }
                read += size;
                digest.update(buf, 0, size);
                bb.clear();
                if (read >= limit) {
                    break;
                }
            }
            return this;
        } finally {
            if (fcin != null) {
                try {
                    fcin.close();
                } catch (Exception e) {
                }
            } else if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Reset the DigestLight
     * 
     * @return this
     */
    public DigestLight reset() {
        digest.reset();
        finalized = null;
        return this;
    }

    /**
     * 
     * @return the digest
     */
    public byte[] digest() {
        if (finalized == null) {
            MessageDigest dclone;
            try {
                dclone = (MessageDigest) digest.clone();
                finalized = dclone.digest();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                finalized = digest.digest();
            }
        }
        return finalized;
    }

    /**
     * 
     * @return the digest in Base16 format
     */
    public String digestHex() {
        if (finalized == null) {
            MessageDigest dclone;
            try {
                dclone = (MessageDigest) digest.clone();
                finalized = dclone.digest();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                finalized = digest.digest();
            }
        }
        return BaseXx.getBase16(finalized);
    }

    @Override
    public String toString() {
        return digestHex();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DigestLight) {
            DigestLight digestLight = (DigestLight) obj;
            return digestLight.type == type
                    && MessageDigest.isEqual(digest(), digestLight.digest());
        }
        return false;
    }

    @Override
	public int hashCode() {
		return digestHex().hashCode();
	}

	/**
     * @param digest
     *            the digest to compare to
     * @param algo
     *            the associated algorithm
     * @return True if the 2 digests are of the same type and same value
     */
    public boolean equals(String digest, DigestTypeLight algo) {
        return algo == type && digest.equalsIgnoreCase(digestHex());
    }

    /**
     * @param digest
     *            the digest in byte to use
     * @param algo
     *            the associated algorithm
     * @return True if the 2 digests are of the same type and same value
     */
    public boolean equals(byte[] digest, DigestTypeLight algo) {
        return algo == type && MessageDigest.isEqual(digest(), digest);
    }

    /**
     * @param in
     *            the file from which the digest will be computed
     * @param algo
     *            the algorithm to use
     * @return the digest for this File
     * @throws IOException
     *             if any IO error occurs
     */
    public static final DigestLight digestLight(File in, DigestTypeLight algo)
            throws IOException {
        return new DigestLight(algo).update(in, 0, -1);
    }
}
