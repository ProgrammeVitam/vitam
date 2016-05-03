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

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.junit.Test;

import fr.gouv.vitam.common.logging.CommonsLogger;
import fr.gouv.vitam.common.logging.VitamLogger;

@SuppressWarnings("javadoc")
public class CommonsLoggerTest {
    private static final Exception e = new Exception();

    @Test
    public void testIsTraceEnabled() {
        final Log mock = createStrictMock(Log.class);

        expect(mock.isTraceEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        assertTrue(logger.isTraceEnabled());
        verify(mock);
    }

    @Test
    public void testIsDebugEnabled() {
        final Log mock = createStrictMock(Log.class);

        expect(mock.isDebugEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        assertTrue(logger.isDebugEnabled());
        verify(mock);
    }

    @Test
    public void testIsInfoEnabled() {
        final Log mock = createStrictMock(Log.class);

        expect(mock.isInfoEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        assertTrue(logger.isInfoEnabled());
        verify(mock);
    }

    @Test
    public void testIsWarnEnabled() {
        final Log mock = createStrictMock(Log.class);

        expect(mock.isWarnEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        assertTrue(logger.isWarnEnabled());
        verify(mock);
    }

    @Test
    public void testIsErrorEnabled() {
        final Log mock = createStrictMock(Log.class);

        expect(mock.isErrorEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        assertTrue(logger.isErrorEnabled());
        verify(mock);
    }

    @Test
    public void testTrace() {
        final Log mock = createStrictMock(Log.class);

        mock.trace("a");
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.trace("a");
        verify(mock);
    }

    @Test
    public void testTraceWithException() {
        final Log mock = createStrictMock(Log.class);

        mock.trace("a", e);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.trace("a", e);
        verify(mock);
    }

    @Test
    public void testDebug() {
        final Log mock = createStrictMock(Log.class);

        mock.debug("a");
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.debug("a");
        verify(mock);
    }

    @Test
    public void testDebugWithException() {
        final Log mock = createStrictMock(Log.class);

        mock.debug("a", e);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.debug("a", e);
        verify(mock);
    }

    @Test
    public void testInfo() {
        final Log mock = createStrictMock(Log.class);

        mock.info("a");
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.info("a");
        verify(mock);
    }

    @Test
    public void testInfoWithException() {
        final Log mock = createStrictMock(Log.class);

        mock.info("a", e);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.info("a", e);
        verify(mock);
    }

    @Test
    public void testWarn() {
        final Log mock = createStrictMock(Log.class);

        mock.warn("a");
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.warn("a");
        verify(mock);
    }

    @Test
    public void testWarnWithException() {
        final Log mock = createStrictMock(Log.class);

        mock.warn("a", e);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.warn("a", e);
        verify(mock);
    }

    @Test
    public void testError() {
        final Log mock = createStrictMock(Log.class);

        mock.error("a");
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.error("a");
        verify(mock);
    }

    @Test
    public void testErrorWithException() {
        final Log mock = createStrictMock(Log.class);

        mock.error("a", e);
        replay(mock);

        final VitamLogger logger = new CommonsLogger(mock, "foo");
        logger.error("a", e);
        verify(mock);
    }
}
