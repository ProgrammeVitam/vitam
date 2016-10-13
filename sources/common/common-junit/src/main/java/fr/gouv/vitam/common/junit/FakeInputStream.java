package fr.gouv.vitam.common.junit;

import java.io.InputStream;
import java.util.Random;

/**
 * Fake InputStream
 */
public class FakeInputStream extends InputStream {
    private static final int BYTE_VALUE_LIMIT = 126;
    private long limit;
    private long read = 0;
    private final boolean block;
    private final Random random = new Random();

    /**
     * Constructor of Fake InputStream
     *
     * @param limit the total size of the InputStream
     * @param block True means the byte are read per block and False means one by one read
     */
    public FakeInputStream(long limit, boolean block) {
        this.limit = limit;
        this.block = block;
    }

    private byte getValue() {
        return (byte) random.nextInt(BYTE_VALUE_LIMIT);
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
            int val = read();
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
        int max = Math.min(available(), len);
        limit -= max;
        read += max;
        for (int i = 0; i < max; i++) {
            b[off + i] = getValue();
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
     *
     * @return the number of truely read bytes
     */
    public long readCount() {
        return read;
    }
}
