/**
 * This file is part of VITAM Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * All VITAM Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * VITAM is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * VITAM . If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gouv.vitam.common.logging;

import ch.qos.logback.classic.Logger;

/**
 * logger using SLF4J from LOGBACK
 *
 * 
 *
 */
public class LogbackLogger extends AbstractVitamLogger {
    private static final long serialVersionUID = -7588688826950608830L;
    /**
     * Internal logger
     */
    private final Logger logger;

    /**
     *
     * @param logger
     */
    public LogbackLogger(final Logger logger) {
        super(logger.getName());
        this.logger = logger;
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(final String msg) {
        if (logger.isTraceEnabled()) {
            logger.trace(getLoggerMethodAndLine() + msg);
        }
    }

    @Override
    public void trace(final String format, final Object arg) {
        if (logger.isTraceEnabled()) {
            logger.trace(getLoggerMethodAndLine() + format, arg);
        }
    }

    @Override
    public void trace(final String format, final Object argA, final Object argB) {
        if (logger.isTraceEnabled()) {
            logger.trace(getLoggerMethodAndLine() + format, argA, argB);
        }
    }

    @Override
    public void trace(final String format, final Object... argArray) {
        if (logger.isTraceEnabled()) {
            logger.trace(getLoggerMethodAndLine() + format, argArray);
        }
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(getLoggerMethodAndLine() + msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(final String msg) {
        if (logger.isDebugEnabled()) {
            logger.debug(getLoggerMethodAndLine() + msg);
        }
    }

    @Override
    public void debug(final String format, final Object arg) {
        if (logger.isDebugEnabled()) {
            logger.debug(getLoggerMethodAndLine() + format, arg);
        }
    }

    @Override
    public void debug(final String format, final Object argA, final Object argB) {
        if (logger.isDebugEnabled()) {
            logger.debug(getLoggerMethodAndLine() + format, argA, argB);
        }
    }

    @Override
    public void debug(final String format, final Object... argArray) {
        if (logger.isDebugEnabled()) {
            logger.debug(getLoggerMethodAndLine() + format, argArray);
        }
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(getLoggerMethodAndLine() + msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(final String msg) {
        if (logger.isInfoEnabled()) {
            logger.info(getLoggerMethodAndLine() + msg);
        }
    }

    @Override
    public void info(final String format, final Object arg) {
        if (logger.isInfoEnabled()) {
            logger.info(getLoggerMethodAndLine() + format, arg);
        }
    }

    @Override
    public void info(final String format, final Object argA, final Object argB) {
        if (logger.isInfoEnabled()) {
            logger.info(getLoggerMethodAndLine() + format, argA, argB);
        }
    }

    @Override
    public void info(final String format, final Object... argArray) {
        if (logger.isInfoEnabled()) {
            logger.info(getLoggerMethodAndLine() + format, argArray);
        }
    }

    @Override
    public void info(final String msg, final Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(getLoggerMethodAndLine() + msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(final String msg) {
        if (logger.isWarnEnabled()) {
            logger.warn(getLoggerMethodAndLine() + msg);
        }
    }

    @Override
    public void warn(final String format, final Object arg) {
        if (logger.isWarnEnabled()) {
            logger.warn(getLoggerMethodAndLine() + format, arg);
        }
    }

    @Override
    public void warn(final String format, final Object... argArray) {
        if (logger.isWarnEnabled()) {
            logger.warn(getLoggerMethodAndLine() + format, argArray);
        }
    }

    @Override
    public void warn(final String format, final Object argA, final Object argB) {
        if (logger.isWarnEnabled()) {
            logger.warn(getLoggerMethodAndLine() + format, argA, argB);
        }
    }

    @Override
    public void warn(final String msg, final Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn(getLoggerMethodAndLine() + msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(final String msg) {
        logger.error(getLoggerMethodAndLine() + msg);
    }

    @Override
    public void error(final String format, final Object arg) {
        logger.error(getLoggerMethodAndLine() + format, arg);
    }

    @Override
    public void error(final String format, final Object argA, final Object argB) {
        logger.error(getLoggerMethodAndLine() + format, argA, argB);
    }

    @Override
    public void error(final String format, final Object... argArray) {
        logger.error(getLoggerMethodAndLine() + format, argArray);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        logger.error(getLoggerMethodAndLine() + msg, t);
    }
    // logger.warn(getLoggerMethodAndLine() + format, arg1);
}
