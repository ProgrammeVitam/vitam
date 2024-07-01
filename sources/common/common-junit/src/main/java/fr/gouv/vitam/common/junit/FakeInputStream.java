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
package fr.gouv.vitam.common.junit;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Fake InputStream: test class only
 */
public class FakeInputStream extends InputStream {

    private static final int BYTE_VALUE_LIMIT = 126;
    private long limit;
    private long read = 0;
    private final boolean block;
    private final SecureRandom random = new SecureRandom();
    private final boolean useRandom;

    /**
     * Constructor of Fake InputStream
     * <br/>
     * <br/>
     * <b>Preferred constructor</b>
     *
     * @param limit the total size of the InputStream
     */
    public FakeInputStream(long limit) {
        this(limit, true, false);
    }

    /**
     * Constructor of Fake InputStream
     *
     * @param limit the total size of the InputStream
     * @param block True means the byte are read per block and False means one by one read
     */
    public FakeInputStream(long limit, boolean block) {
        this(limit, block, false);
    }

    /**
     * Constructor of Fake InputStream
     *
     * @param limit the total size of the InputStream
     * @param block True means the byte are read per block and False means one by one read
     * @param useRandom True use random values for each bytes, else 42 for each
     */
    public FakeInputStream(long limit, boolean block, boolean useRandom) {
        this.limit = limit;
        this.block = block;
        this.useRandom = useRandom;
    }

    private final byte getValue() {
        if (useRandom) {
            return (byte) random.nextInt(BYTE_VALUE_LIMIT);
        }
        return 42;
    }

    @Override
    public int read() {
        if (limit <= 0) {
            return -1;
        }
        limit--;
        read++;
        return getValue();
    }

    @Override
    public int available() {
        if (limit > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) limit;
    }

    @Override
    public void close() {
        limit = 0;
    }

    private int readBlockUnitary(byte[] b, int off, int len) {
        for (int i = 0; i < len; i++) {
            final int val = read();
            if (val < 0) {
                if (i == 0) {
                    return -1;
                }
                return i;
            }
            b[off + i] = (byte) val;
        }
        return len;
    }

    private int readBlock(byte[] b, int off, int len) {
        if (limit <= 0) {
            return -1;
        }
        final int max = Math.min(available(), len);
        limit -= max;
        read += max;
        if (!useRandom) {
            Arrays.fill(b, off, off + max, getValue());
        } else {
            for (int i = 0; i < max; i++) {
                b[off + i] = getValue();
            }
        }
        return max;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (block) {
            return readBlock(b, off, len);
        } else {
            return readBlockUnitary(b, off, len);
        }
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    /**
     * @return the number of truely read bytes
     */
    public long readCount() {
        return read;
    }
}
