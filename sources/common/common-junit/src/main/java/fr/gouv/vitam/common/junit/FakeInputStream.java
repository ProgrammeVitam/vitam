package fr.gouv.vitam.common.junit;

import java.io.InputStream;
import java.util.Random;

import com.google.common.util.concurrent.FakeTimeLimiter;

/**
 * Fake InputStream
 */
public class FakeInputStream extends InputStream {
    private static final int BYTE_VALUE_LIMIT = 126;
    private long limit;
    private long read = 0;
    private final boolean block;
    private final Random random = new Random();
    private final boolean useRandom;

    /**
     * Constructor of Fake InputStream
     *<br/>
     *<br/>
     *<b>Preferred constructor</b>
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

    private byte getValue() {
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
