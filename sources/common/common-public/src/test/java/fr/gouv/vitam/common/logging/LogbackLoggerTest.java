/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.common.logging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import javax.management.RuntimeErrorException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class LogbackLoggerTest {
    private static final Exception e = new Exception();
    private static final PrintStream out = System.out; // NOSONAR since Logger test
    private static final StringBuilder buf = new StringBuilder();

    @BeforeClass
    public static void setUpBeforeClass() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.TRACE));
        final VitamLogger logger = VitamLoggerFactory.getInstance(LogbackLoggerTest.class);
        logger.debug("Start Logback test", new Exception("test", new Exception("original")));
        try {
            System.setOut(new PrintStream(new OutputStream() {
                @Override
                public void write(final int b) {
                    buf.append((char) b);
                }
            }, true, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeErrorException(new Error(e));
        }
        e.setStackTrace(new StackTraceElement[] {new StackTraceElement("n1", "n2", "n3", 4)});
    }

    @AfterClass
    public static void tearDownAfterClass() {
        System.setErr(out);
    }

    @Test
    public void testIsTraceEnabled() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.TRACE));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo");
        assertTrue(logger.isTraceEnabled());
        buf.setLength(0);
        logger.trace("a");
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.trace("a", e);
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.trace("", new Object());
        logger.trace("", new Object(), new Object());
        logger.trace("", new Object(), new Object(), new Object());
        buf.setLength(0);
        logger.isEnabled(VitamLogLevel.TRACE);
    }

    @Test
    public void testIsDebugEnabled() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.DEBUG));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo");
        assertTrue(logger.isDebugEnabled());
        buf.setLength(0);
        logger.debug("a");
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.debug("a", e);
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.debug("", new Object());
        logger.debug("", new Object(), new Object());
        logger.debug("", new Object(), new Object(), new Object());
        buf.setLength(0);
        logger.isEnabled(VitamLogLevel.DEBUG);
    }

    @Test
    public void testIsInfoEnabled() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.INFO));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo");
        assertTrue(logger.isInfoEnabled());
        buf.setLength(0);
        logger.info("a");
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.info("a", e);
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.info("", new Object());
        logger.info("", new Object(), new Object());
        logger.info("", new Object(), new Object(), new Object());
        buf.setLength(0);
        logger.isEnabled(VitamLogLevel.INFO);
    }

    @Test
    public void testIsWarnEnabled() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.WARN));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo");
        assertTrue(logger.isWarnEnabled());
        buf.setLength(0);
        logger.warn("a");
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.warn("a", e);
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.warn("", new Object());
        logger.warn("", new Object(), new Object());
        logger.warn("", new Object(), new Object(), new Object());
        buf.setLength(0);
        logger.isEnabled(VitamLogLevel.WARN);
    }

    @Test
    public void testIsErrorEnabled() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.ERROR));
        VitamLogger logger = VitamLoggerFactory.getInstance("foo");
        assertTrue(logger.isErrorEnabled());
        buf.setLength(0);
        logger.error("a");
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.error("a", e);
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.error("", new Object());
        logger.error("", new Object(), new Object());
        logger.error("", new Object(), new Object(), new Object());
        logger.error(new Throwable("a"));
        buf.setLength(0);
        assertTrue(logger.isEnabled(VitamLogLevel.ERROR));

        // Check varous calls
        logger.trace(new Throwable("a"));
        logger.debug(new Throwable("a"));
        logger.info(new Throwable("a"));
        logger.warn(new Throwable("a"));
        logger.log(VitamLogLevel.TRACE, "text", new Throwable("a"));
        logger.log(VitamLogLevel.DEBUG, "text", new Throwable("a"));
        logger.log(VitamLogLevel.INFO, "text", new Throwable("a"));
        logger.log(VitamLogLevel.WARN, "text", new Throwable("a"));
        logger.log(VitamLogLevel.TRACE, "text");
        logger.log(VitamLogLevel.DEBUG, "text");
        logger.log(VitamLogLevel.INFO, "text");
        logger.log(VitamLogLevel.WARN, "text");
        logger.log(VitamLogLevel.TRACE, new Throwable("a"));
        logger.log(VitamLogLevel.DEBUG, new Throwable("a"));
        logger.log(VitamLogLevel.INFO, new Throwable("a"));
        logger.log(VitamLogLevel.WARN, new Throwable("a"));
        logger.log(VitamLogLevel.TRACE, "text", new Object());
        logger.log(VitamLogLevel.DEBUG, "text", new Object());
        logger.log(VitamLogLevel.INFO, "text", new Object());
        logger.log(VitamLogLevel.WARN, "text", new Object());
        logger.log(VitamLogLevel.TRACE, "text", new Object(), new Object());
        logger.log(VitamLogLevel.DEBUG, "text", new Object(), new Object());
        logger.log(VitamLogLevel.INFO, "text", new Object(), new Object());
        logger.log(VitamLogLevel.WARN, "text", new Object(), new Object());
        logger.log(VitamLogLevel.TRACE, "text", new Object(), new Object(), new Object());
        logger.log(VitamLogLevel.DEBUG, "text", new Object(), new Object(), new Object());
        logger.log(VitamLogLevel.INFO, "text", new Object(), new Object(), new Object());
        logger.log(VitamLogLevel.WARN, "text", new Object(), new Object(), new Object());
        assertTrue(buf.length() == 0);

        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.TRACE));
        logger = VitamLoggerFactory.getInstance("foo");
        assertTrue(logger.isEnabled(VitamLogLevel.TRACE));

        // Check varous calls
        logger.trace(new Throwable("a"));
        logger.debug(new Throwable("a"));
        logger.info(new Throwable("a"));
        logger.warn(new Throwable("a"));
        logger.log(VitamLogLevel.TRACE, "text", new Throwable("a"));
        logger.log(VitamLogLevel.DEBUG, "text", new Throwable("a"));
        logger.log(VitamLogLevel.INFO, "text", new Throwable("a"));
        logger.log(VitamLogLevel.WARN, "text", new Throwable("a"));
        logger.log(VitamLogLevel.ERROR, "text", new Throwable("a"));
        logger.log(VitamLogLevel.TRACE, "text");
        logger.log(VitamLogLevel.DEBUG, "text");
        logger.log(VitamLogLevel.INFO, "text");
        logger.log(VitamLogLevel.WARN, "text");
        logger.log(VitamLogLevel.ERROR, "text");
        logger.log(VitamLogLevel.TRACE, new Throwable("a"));
        logger.log(VitamLogLevel.DEBUG, new Throwable("a"));
        logger.log(VitamLogLevel.INFO, new Throwable("a"));
        logger.log(VitamLogLevel.WARN, new Throwable("a"));
        logger.log(VitamLogLevel.ERROR, new Throwable("a"));
        logger.log(VitamLogLevel.TRACE, "text", new Object());
        logger.log(VitamLogLevel.DEBUG, "text", new Object());
        logger.log(VitamLogLevel.INFO, "text", new Object());
        logger.log(VitamLogLevel.WARN, "text", new Object());
        logger.log(VitamLogLevel.ERROR, "text", new Object());
        logger.log(VitamLogLevel.TRACE, "text", new Object(), new Object());
        logger.log(VitamLogLevel.DEBUG, "text", new Object(), new Object());
        logger.log(VitamLogLevel.INFO, "text", new Object(), new Object());
        logger.log(VitamLogLevel.WARN, "text", new Object(), new Object());
        logger.log(VitamLogLevel.ERROR, "text", new Object(), new Object());
        logger.log(VitamLogLevel.TRACE, "text", new Object(), new Object(), new Object());
        logger.log(VitamLogLevel.DEBUG, "text", new Object(), new Object(), new Object());
        logger.log(VitamLogLevel.INFO, "text", new Object(), new Object(), new Object());
        logger.log(VitamLogLevel.WARN, "text", new Object(), new Object(), new Object());
        logger.log(VitamLogLevel.ERROR, "text", new Object(), new Object(), new Object());
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        try {
            ((LogbackLogger) logger).readResolve();
        } catch (final ObjectStreamException e1) { // NOSONAR
            fail("Shound not raized an exception");
        }
        AbstractVitamLogger.simpleClassName(LogbackLoggerTest.class);
        AbstractVitamLogger.simpleClassName(new Object());
        AbstractVitamLogger.getMessagePrepend();
    }


    @Test
    public void testTimeTrace() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.INFO));
        VitamLogger logger = VitamLoggerFactory.getInstance("foo");
        assertTrue(logger.isInfoEnabled());
        buf.setLength(0);
        logger.timeInfo("a");
        assertTrue(buf.indexOf(AbstractVitamLogger.TIME_TRACE_PREFIX) > 0);
        buf.setLength(0);
        logger.timeInfo("", new Object());
        assertTrue(buf.indexOf(AbstractVitamLogger.TIME_TRACE_PREFIX) > 0);
        buf.setLength(0);
        logger.timeInfo("", new Object(), new Object());
        assertTrue(buf.indexOf(AbstractVitamLogger.TIME_TRACE_PREFIX) > 0);
        buf.setLength(0);
        logger.timeInfo("", new Object(), new Object(), new Object());
        assertTrue(buf.indexOf(AbstractVitamLogger.TIME_TRACE_PREFIX) > 0);
        buf.setLength(0);
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.WARN));
        logger = VitamLoggerFactory.getInstance("foo");
        assertFalse(logger.isInfoEnabled());
        buf.setLength(0);
        logger.timeInfo("a");
        assertFalse(buf.indexOf(AbstractVitamLogger.TIME_TRACE_PREFIX) > 0);
        buf.setLength(0);
        logger.timeInfo("", new Object());
        assertFalse(buf.indexOf(AbstractVitamLogger.TIME_TRACE_PREFIX) > 0);
        buf.setLength(0);
    }

}
