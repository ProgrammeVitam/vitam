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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.gouv.vitam.common.logging.LogbackLogger;
import fr.gouv.vitam.common.logging.LogbackLoggerFactory;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

@SuppressWarnings("javadoc")
public class LogbackLoggerFactoryTest {

    @Test
    public void testCreation() {
        VitamLoggerFactory.setDefaultFactory(new LogbackLoggerFactory(VitamLogLevel.WARN));
        assertTrue(VitamLoggerFactory.getDefaultFactory() instanceof LogbackLoggerFactory);
        assertEquals(VitamLogLevel.WARN, VitamLoggerFactory.currentLevel);
        VitamLogger logger0 = VitamLoggerFactory.getDefaultFactory().newInstance("foo");
        assertTrue(logger0 instanceof LogbackLogger);
        assertEquals("foo", logger0.name());
        assertFalse(logger0.isTraceEnabled());
        VitamLoggerFactory.setLogLevel(VitamLogLevel.TRACE);
        assertTrue(logger0.isTraceEnabled());
        logger0 = VitamLoggerFactory.getDefaultFactory().newInstance("foo");
        assertTrue(logger0.isTraceEnabled());
    }
}
