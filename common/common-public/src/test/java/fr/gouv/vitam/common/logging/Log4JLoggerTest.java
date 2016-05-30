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

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import javax.management.RuntimeErrorException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class Log4JLoggerTest {
    private static final Exception e = new Exception();
    private static final PrintStream out = System.out; // NOSONAR since Logger test
    private static final StringBuilder buf = new StringBuilder();

    @BeforeClass
    public static void setUpBeforeClass() {
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
        VitamLoggerFactory.setDefaultFactory(new Log4JLoggerFactory(VitamLogLevel.TRACE));
        VitamLogger logger = VitamLoggerFactory.getInstance("foo");
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

        VitamLoggerFactory.setDefaultFactory(new Log4JLoggerFactory(VitamLogLevel.DEBUG));
        logger = VitamLoggerFactory.getInstance("foo");
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

        VitamLoggerFactory.setDefaultFactory(new Log4JLoggerFactory(VitamLogLevel.INFO));
        logger = VitamLoggerFactory.getInstance("foo");
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

        VitamLoggerFactory.setDefaultFactory(new Log4JLoggerFactory(VitamLogLevel.WARN));
        logger = VitamLoggerFactory.getInstance("foo");
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

        VitamLoggerFactory.setDefaultFactory(new Log4JLoggerFactory(VitamLogLevel.ERROR));
        logger = VitamLoggerFactory.getInstance("foo");
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

}
