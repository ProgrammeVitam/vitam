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
package fr.gouv.vitam.common.logging;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.RuntimeErrorException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JdkLoggerTest {

    private static final Exception e = new Exception();
    private static final PrintStream out = System.out; // NOSONAR since Logger test
    private static final StringBuilder buf = new StringBuilder();

    @BeforeClass
    public static void setUpBeforeClass() {
        try {
            System.setOut(
                new PrintStream(
                    new OutputStream() {
                        @Override
                        public void write(final int b) {
                            buf.append((char) b);
                        }
                    },
                    true,
                    "UTF-8"
                )
            );
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeErrorException(new Error(e));
        }
        e.setStackTrace(new StackTraceElement[] { new StackTraceElement("n1", "n2", "n3", 4) });
    }

    @AfterClass
    public static void tearDownAfterClass() {
        System.setOut(out);
    }

    @Test
    public void testCreation() {
        // Note: partially automatically generated
        new JdkLoggerFactory(VitamLogLevel.TRACE);
        Logger logger0 = Logger.getLogger("'a([(j92O]Xnk>*F) ");
        JdkLogger jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.debug("'a([(j92O]Xnk>*F) ", "'a([(j92O]Xnk>*F) ", jdkLogger0);
        assertEquals("'a([(j92O]Xnk>*F) ", jdkLogger0.name());
        logger0 = Logger.getLogger("sun.reflect.NativeMethodAccessorImpl");
        jdkLogger0 = new JdkLogger(logger0);
        final VitamLogLevel vitamLogLevel0 = VitamLogLevel.TRACE;
        jdkLogger0.log(vitamLogLevel0, "", jdkLogger0);
        assertEquals("sun.reflect.NativeMethodAccessorImpl", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        boolean boolean0 = jdkLogger0.isInfoEnabled();
        logger0 = Logger.getLogger("sun.reflect.NativeMethodAccessorImpl");
        jdkLogger0 = new JdkLogger(logger0);
        Exception exception0 = new Exception("sun.reflect.NativeMethodAccessorImpl");
        exception0.setStackTrace(new StackTraceElement[] { new StackTraceElement("n1", "n2", "n3", 4) });
        jdkLogger0.error("sun.reflect.NativeMethodAccessorImpl", exception0);
        assertEquals("sun.reflect.NativeMethodAccessorImpl", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        Object[] objectArray0 = new Object[0];
        jdkLogger0.error("", objectArray0);
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.error("k'n(W*WnB(ip7/: ", "sun.reflect.GeneratedMethodAccessor11", "k'n(W*WnB(ip7/: ");
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.error("sun.reflect.GeneratedMethodAccessor39", "sun.reflect.GeneratedMethodAccessor39");
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.error("DEBUG");
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getAnonymousLogger();
        Logger logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        exception0 = new Exception("g.6+Eh`xw;&+MV(z");
        exception0.setStackTrace(new StackTraceElement[] { new StackTraceElement("n1", "n2", "n3", 4) });
        jdkLogger0.warn("g.6+Eh`xw;&+MV(z", exception0);
        assertEquals("g.6+Eh`xw;&+MV(z", exception0.getMessage());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        objectArray0 = new Object[1];
        jdkLogger0.warn("DEBUG", objectArray0);
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.warn("", "");
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.warn("P!1G14s4`");
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        exception0 = new Exception("y&]>", (Throwable) null);
        exception0.setStackTrace(new StackTraceElement[] { new StackTraceElement("n1", "n2", "n3", 4) });
        jdkLogger0.info(exception0);
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        objectArray0 = new Object[4];
        jdkLogger0.info("g", objectArray0);
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        final Object object0 = new Object();
        jdkLogger0.info("5!F H", object0, jdkLogger0);
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        jdkLogger0.info("", "");
        assertEquals("", jdkLogger0.name());
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        jdkLogger0.info("TB3RPuq#i");
        assertEquals("", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        exception0 = new Exception();
        exception0.setStackTrace(new StackTraceElement[] { new StackTraceElement("n1", "n2", "n3", 4) });
        jdkLogger0.debug("", exception0);
        assertEquals("java.lang.Exception", exception0.toString());
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        objectArray0 = new Object[5];
        jdkLogger0.debug("", objectArray0);
        assertEquals("", jdkLogger0.name());
        logger0 = Logger.getLogger("detectLoggingBaseLevel");
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.debug("", "detectLoggingBaseLevel");
        assertEquals("detectLoggingBaseLevel", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.debug("#");
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        exception0 = new Exception((Throwable) null);
        exception0.setStackTrace(new StackTraceElement[] { new StackTraceElement("n1", "n2", "n3", 4) });
        jdkLogger0.trace("null", exception0);
        assertEquals("", jdkLogger0.name());
        logger0 = Logger.getLogger("detectLoggingBaseLevel");
        jdkLogger0 = new JdkLogger(logger0);
        objectArray0 = new Object[2];
        jdkLogger0.trace("detectLoggingBaseLevel", objectArray0);
        assertEquals("detectLoggingBaseLevel", jdkLogger0.name());
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        exception0 = new Exception();
        final StackTraceElement[] stackTraceElementArray0 = exception0.getStackTrace();
        jdkLogger0.trace("};Dg`Me}$_cLd}O(}$", (Object[]) stackTraceElementArray0);
        assertEquals("", jdkLogger0.name());
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        exception0 = new Exception("g.6+Eh`xw;&+MV(z");
        exception0.setStackTrace(new StackTraceElement[] { new StackTraceElement("n1", "n2", "n3", 4) });
        jdkLogger0.trace("g.6+Eh`xw;&+MV(z", jdkLogger0, exception0);
        assertEquals("java.lang.Exception: g.6+Eh`xw;&+MV(z", exception0.toString());
        logger0 = Logger.getLogger("");
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.trace("", jdkLogger0);
        assertEquals("", jdkLogger0.name());
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        jdkLogger0.trace("g.6+Eh`xw;&+MV(z");
        assertEquals("", jdkLogger0.name());
        logger0 = Logger.getGlobal();
        jdkLogger0 = new JdkLogger(logger0);
        jdkLogger0.warn("", jdkLogger0, "");
        assertEquals("global", jdkLogger0.name());
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        boolean0 = jdkLogger0.isWarnEnabled();
        assertTrue(boolean0);
        logger0 = Logger.getAnonymousLogger();
        logger1 = logger0.getParent();
        jdkLogger0 = new JdkLogger(logger1);
        boolean0 = jdkLogger0.isErrorEnabled();
        assertTrue(boolean0);
        logger0 = Logger.getLogger("");
        jdkLogger0 = new JdkLogger(logger0);
        exception0 = new Exception();
        exception0.setStackTrace(new StackTraceElement[] { new StackTraceElement("n1", "n2", "n3", 4) });
        jdkLogger0.info("v>S.;58(1\"", exception0);
        assertEquals("java.lang.Exception", exception0.toString());
    }

    @Test
    public void testIsTraceEnabled() {
        VitamLoggerFactory.setDefaultFactory(new JdkLoggerFactory(VitamLogLevel.TRACE));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo1");
        assertTrue(logger.isTraceEnabled());
        buf.setLength(0);
        logger.trace("a");
        buf.setLength(0);
        logger.trace("a", e);
        buf.setLength(0);
        logger.trace("", new Object());
        logger.trace("", new Object(), new Object());
        logger.trace("", new Object(), new Object(), new Object());
        buf.setLength(0);
        logger.setLevel(VitamLogLevel.ERROR);
        assertFalse(logger.isTraceEnabled());
    }

    @Test
    public void testIsDebugEnabled() {
        VitamLoggerFactory.setDefaultFactory(new JdkLoggerFactory(VitamLogLevel.DEBUG));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo2");
        assertTrue(logger.isDebugEnabled());
        buf.setLength(0);
        logger.debug("a");
        buf.setLength(0);
        logger.debug("a", e);
        buf.setLength(0);
        logger.debug("", new Object());
        logger.debug("", new Object(), new Object());
        logger.debug("", new Object(), new Object(), new Object());
        buf.setLength(0);
    }

    @Test
    public void testIsInfoEnabled() {
        VitamLoggerFactory.setDefaultFactory(new JdkLoggerFactory(VitamLogLevel.INFO));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo3");
        assertTrue(logger.isInfoEnabled());
        buf.setLength(0);
        logger.info("a");
        buf.setLength(0);
        logger.info("a", e);
        buf.setLength(0);
        logger.info("", new Object());
        logger.info("", new Object(), new Object());
        logger.info("", new Object(), new Object(), new Object());
        buf.setLength(0);
    }

    @Test
    public void testIsWarnEnabled() {
        VitamLoggerFactory.setDefaultFactory(new JdkLoggerFactory(VitamLogLevel.WARN));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo4");
        assertTrue(logger.isWarnEnabled());
        buf.setLength(0);
        logger.warn("a");
        buf.setLength(0);
        logger.warn("a", e);
        buf.setLength(0);
        logger.warn("", new Object());
        logger.warn("", new Object(), new Object());
        logger.warn("", new Object(), new Object(), new Object());
        buf.setLength(0);
    }

    @Test
    public void testIsErrorEnabled() {
        VitamLoggerFactory.setDefaultFactory(new JdkLoggerFactory(VitamLogLevel.ERROR));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo5");
        assertTrue(logger.isErrorEnabled());
        buf.setLength(0);
        logger.error("a");
        buf.setLength(0);
        logger.error("a", e);
        buf.setLength(0);
        logger.error("", new Object());
        logger.error("", new Object(), new Object());
        logger.error("", new Object(), new Object(), new Object());
        buf.setLength(0);
    }

    @Test
    public void testTimeTrace() {
        VitamLoggerFactory.setDefaultFactory(new JdkLoggerFactory(VitamLogLevel.INFO));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo6");
        assertTrue(logger.isInfoEnabled());
        buf.setLength(0);
        logger.timeInfo("a");
        buf.setLength(0);
        logger.timeInfo("", new Object());
        logger.timeInfo("", new Object(), new Object());
        logger.timeInfo("", new Object(), new Object(), new Object());
        buf.setLength(0);
    }
}
