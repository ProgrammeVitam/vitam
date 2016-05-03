/*
 * Copyright 2012 The Netty Project The Netty Project licenses this file to you
 * under the Apache License, version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at: http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package fr.gouv.vitam.common.logging;

import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Logger factory which creates an
 * <a href="http://commons.apache.org/logging/">Apache Commons Logging</a>
 * logger.
 */
public class CommonsLoggerFactory extends VitamLoggerFactory {

    /**
     * @param level
     *            default level (can be null, then from the configuration
     *            default file)
     */
    public CommonsLoggerFactory(final VitamLogLevel level) {
        super(level);
        seLevelSpecific(currentLevel);
    }

    @Override
    public VitamLogger newInstance(final String name) {
        return new CommonsLogger(LogFactory.getLog(name), name);
    }

    @Override
    protected void seLevelSpecific(final VitamLogLevel level) {
        // XXX FIXME does not work for Apache Commons Logger
        switch (level) {
            case TRACE:
                LogFactory.getFactory().setAttribute(LogFactory.PRIORITY_KEY,
                        Level.FINEST);
                break;
            case DEBUG:
                LogFactory.getFactory().setAttribute(LogFactory.PRIORITY_KEY, Level.FINE);
                break;
            case INFO:
                LogFactory.getFactory().setAttribute(LogFactory.PRIORITY_KEY, Level.INFO);
                break;
            case WARN:
                LogFactory.getFactory().setAttribute(LogFactory.PRIORITY_KEY,
                        Level.WARNING);
                break;
            case ERROR:
                LogFactory.getFactory().setAttribute(LogFactory.PRIORITY_KEY,
                        Level.SEVERE);
                break;
            default:
                LogFactory.getFactory().setAttribute(LogFactory.PRIORITY_KEY,
                        Level.WARNING);
                break;
        }
    }

    @Override
    protected VitamLogLevel getLevelSpecific() {
        final Log logger = LogFactory.getFactory().getInstance("foo");
        if (logger.isTraceEnabled()) {
            return VitamLogLevel.TRACE;
        } else if (logger.isDebugEnabled()) {
            return VitamLogLevel.DEBUG;
        } else if (logger.isInfoEnabled()) {
            return VitamLogLevel.INFO;
        } else if (logger.isWarnEnabled()) {
            return VitamLogLevel.WARN;
        } else if (logger.isErrorEnabled()) {
            return VitamLogLevel.ERROR;
        }
        return null;
    }
}
