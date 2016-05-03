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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.gouv.vitam.common.logging.JdkLogger;
import fr.gouv.vitam.common.logging.JdkLoggerFactory;
import fr.gouv.vitam.common.logging.VitamLogger;

@SuppressWarnings("javadoc")
public class JdkLoggerFactoryTest {

    @Test
    public void testCreation() {
        final VitamLogger logger = new JdkLoggerFactory(null).newInstance("foo");
        assertTrue(logger instanceof JdkLogger);
        assertEquals("foo", logger.name());
    }
}
