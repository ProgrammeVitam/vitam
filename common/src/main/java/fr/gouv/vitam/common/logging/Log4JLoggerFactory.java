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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Logger factory which creates an
 * <a href="http://logging.apache.org/log4j/1.2/index.html">Apache Log4J</a>
 * logger.
 */
public class Log4JLoggerFactory extends VitamLoggerFactory {

    /**
     * @param level
     */
    public Log4JLoggerFactory(final VitamLogLevel level) {
        super(level);
        seLevelSpecific(currentLevel);
    }

    @Override
    public VitamLogger newInstance(final String name) {
        return new Log4JLogger(Logger.getLogger(name));
    }

    @Override
    protected void seLevelSpecific(final VitamLogLevel level) {
        final Logger logger = Logger.getRootLogger();
        switch (level) {
            case TRACE:
                logger.setLevel(Level.TRACE);
                break;
            case DEBUG:
                logger.setLevel(Level.DEBUG);
                break;
            case INFO:
                logger.setLevel(Level.INFO);
                break;
            case WARN:
                logger.setLevel(Level.WARN);
                break;
            case ERROR:
                logger.setLevel(Level.ERROR);
                break;
            default:
                logger.setLevel(Level.WARN);
                break;
        }
    }

    @Override
    protected VitamLogLevel getLevelSpecific() {
        final Logger logger = Logger.getRootLogger();
        final Level level = logger.getLevel();
        if (level == Level.TRACE || level == Level.ALL) {
            return VitamLogLevel.TRACE;
        } else if (level == Level.DEBUG) {
            return VitamLogLevel.DEBUG;
        } else if (level == Level.INFO) {
            return VitamLogLevel.INFO;
        } else if (level == Level.WARN) {
            return VitamLogLevel.WARN;
        } else if (level == Level.ERROR || level == Level.FATAL) {
            return VitamLogLevel.ERROR;
        }
        return null;
    }

}
