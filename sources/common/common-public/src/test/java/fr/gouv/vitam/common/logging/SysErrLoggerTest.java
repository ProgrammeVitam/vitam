package fr.gouv.vitam.common.logging;

import static org.junit.Assert.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import javax.management.RuntimeErrorException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SysErrLoggerTest {
    private static final String NOT_EMPTY = "Not empty";
    private static PrintStream err;
    private static final StringBuilder buf = new StringBuilder();

    @BeforeClass
    public static void setUpBeforeClass() {
        err = System.err; // NOSONAR since Logger test
        try {
            System.setErr(new PrintStream(new OutputStream() {
                @Override
                public void write(final int b) {
                    buf.append((char) b);
                }
            }, true, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeErrorException(new Error(e));
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        System.setErr(err);
    }

    @Test
    public void testSyserr() {
        SysErrLogger.FAKE_LOGGER.ignoreLog(new Exception("Fake exception"));
        assertTrue(buf.length() == 0);
        SysErrLogger.FAKE_LOGGER.syserr(NOT_EMPTY);
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        SysErrLogger.FAKE_LOGGER.syserr();
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        SysErrLogger.FAKE_LOGGER.syserr(NOT_EMPTY, new Exception("Fake exception"));
        assertTrue(buf.length() > NOT_EMPTY.length() + 5);
        buf.setLength(0);
    }

}
