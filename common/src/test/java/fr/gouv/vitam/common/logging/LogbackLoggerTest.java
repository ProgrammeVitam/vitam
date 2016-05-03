/*
 * Copyright 2012 The Netty Project
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package fr.gouv.vitam.common.logging;

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.logging.LogbackLoggerFactory;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

@SuppressWarnings("javadoc")
public class LogbackLoggerTest {
    private static final Exception e = new Exception();
    private static final PrintStream out = System.out;
    private static final StringBuffer buf = new StringBuffer();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            System.setOut(new PrintStream(new OutputStream() {
                @Override
                public void write(final int b) {
                    buf.append((char) b);
                }
            }, true, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
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
    }

    @Test
    public void testIsErrorEnabled() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.ERROR));
        final VitamLogger logger = VitamLoggerFactory.getInstance("foo");
        assertTrue(logger.isErrorEnabled());
        buf.setLength(0);
        logger.error("a");
        assertTrue(buf.length() > 0);
        buf.setLength(0);
        logger.error("a", e);
        assertTrue(buf.length() > 0);
        buf.setLength(0);
    }

}
