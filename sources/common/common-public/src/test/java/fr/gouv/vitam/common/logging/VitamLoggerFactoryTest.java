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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("javadoc")
public class VitamLoggerFactoryTest {

    private static final Exception e = new Exception();
    private VitamLoggerFactory oldLoggerFactory;
    private VitamLogger mock;

    @Before
    public void init() {
        oldLoggerFactory = VitamLoggerFactory.getDefaultFactory();
        final VitamLoggerFactory mockFactory = createMock(VitamLoggerFactory.class);
        mock = createStrictMock(VitamLogger.class);
        expect(mockFactory.newInstance("mock")).andReturn(mock).anyTimes();
        replay(mockFactory);
        VitamLoggerFactory.setDefaultFactory(mockFactory);
    }

    @After
    public void destroy() {
        reset(mock);
        VitamLoggerFactory.setDefaultFactory(oldLoggerFactory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNullDefaultFactory() {
        VitamLoggerFactory.setDefaultFactory(null);
    }

    @Test
    public void shouldGetInstance() {
        VitamLoggerFactory.setDefaultFactory(oldLoggerFactory);

        final String helloWorld = "Hello, world!";

        final VitamLogger one = VitamLoggerFactory.getInstance("helloWorld");
        final VitamLogger two = VitamLoggerFactory.getInstance(helloWorld.getClass());

        assertNotNull(one);
        assertNotNull(two);
        assertNotSame(one, two);
        final VitamLogLevel logLevel = VitamLoggerFactory.getLogLevel();
        VitamLoggerFactory.setLogLevel(VitamLogLevel.ERROR);
        assertEquals(VitamLogLevel.ERROR, VitamLoggerFactory.getLogLevel());
        VitamLoggerFactory.setLogLevel(logLevel);
    }

    @Test
    public void testIsTraceEnabled() {
        expect(mock.isTraceEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        assertTrue(logger.isTraceEnabled());
        verify(mock);
    }

    @Test
    public void testIsDebugEnabled() {
        expect(mock.isDebugEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        assertTrue(logger.isDebugEnabled());
        verify(mock);
    }

    @Test
    public void testIsInfoEnabled() {
        expect(mock.isInfoEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        assertTrue(logger.isInfoEnabled());
        verify(mock);
    }

    @Test
    public void testIsWarnEnabled() {
        expect(mock.isWarnEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        assertTrue(logger.isWarnEnabled());
        verify(mock);
    }

    @Test
    public void testIsErrorEnabled() {
        expect(mock.isErrorEnabled()).andReturn(true);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        assertTrue(logger.isErrorEnabled());
        verify(mock);
    }

    @Test
    public void testTrace() {
        mock.trace("a");
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.trace("a");
        verify(mock);
    }

    @Test
    public void testTraceWithException() {
        mock.trace("a", e);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.trace("a", e);
        verify(mock);
    }

    @Test
    public void testDebug() {
        mock.debug("a");
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.debug("a");
        verify(mock);
    }

    @Test
    public void testDebugWithException() {
        mock.debug("a", e);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.debug("a", e);
        verify(mock);
    }

    @Test
    public void testInfo() {
        mock.info("a");
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.info("a");
        verify(mock);
    }

    @Test
    public void testInfoWithException() {
        mock.info("a", e);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.info("a", e);
        verify(mock);
    }

    @Test
    public void testWarn() {
        mock.warn("a");
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.warn("a");
        verify(mock);
    }

    @Test
    public void testWarnWithException() {
        mock.warn("a", e);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.warn("a", e);
        verify(mock);
    }

    @Test
    public void testError() {
        mock.error("a");
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.error("a");
        verify(mock);
    }

    @Test
    public void testErrorWithException() {
        mock.error("a", e);
        replay(mock);

        final VitamLogger logger = VitamLoggerFactory.getInstance("mock");
        logger.error("a", e);
        verify(mock);
    }
}
