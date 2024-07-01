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

import ch.qos.logback.classic.Logger;

/**
 * logger using SLF4J from Logback
 */
final class LogbackLogger extends AbstractVitamLogger {

    private static final long serialVersionUID = -7588688826950608830L;
    /**
     * Internal logger
     */
    private final transient Logger logger; // NOSONAR keep it non static

    /**
     * @param logger the Logger instance
     */
    public LogbackLogger(final Logger logger) {
        super(logger.getName());
        this.logger = logger;
    }

    @Override
    public void setLevel(VitamLogLevel level) {
        LogbackLoggerFactory.loggerSetLevel(logger, level);
    }

    @Override
    public VitamLogLevel getLevel() {
        return LogbackLoggerFactory.loggerGetLevel(logger);
    }

    @Override
    public void timeInfo(String msg) {
        if (logger.isInfoEnabled()) {
            logger.info(TIME_TRACE_PREFIX + getMessagePrepend() + msg);
        }
    }

    @Override
    public void timeInfo(String format, Object... arguments) {
        if (logger.isInfoEnabled()) {
            logger.info(TIME_TRACE_PREFIX + getMessagePrepend() + format, arguments);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(getMessagePrepend(), t);
        }
    }

    @Override
    public void trace(final String msg) {
        if (logger.isTraceEnabled()) {
            logger.trace(getMessagePrepend() + msg);
        }
    }

    @Override
    public void trace(final String format, final Object arg) {
        if (logger.isTraceEnabled()) {
            logger.trace(getMessagePrepend() + format, arg);
        }
    }

    @Override
    public void trace(final String format, final Object argA, final Object argB) {
        if (logger.isTraceEnabled()) {
            logger.trace(getMessagePrepend() + format, argA, argB);
        }
    }

    @Override
    public void trace(final String format, final Object... argArray) {
        if (logger.isTraceEnabled()) {
            logger.trace(getMessagePrepend() + format, argArray);
        }
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(getMessagePrepend() + msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(getMessagePrepend(), t);
        }
    }

    @Override
    public void debug(final String msg) {
        if (logger.isDebugEnabled()) {
            logger.debug(getMessagePrepend() + msg);
        }
    }

    @Override
    public void debug(final String format, final Object arg) {
        if (logger.isDebugEnabled()) {
            logger.debug(getMessagePrepend() + format, arg);
        }
    }

    @Override
    public void debug(final String format, final Object argA, final Object argB) {
        if (logger.isDebugEnabled()) {
            logger.debug(getMessagePrepend() + format, argA, argB);
        }
    }

    @Override
    public void debug(final String format, final Object... argArray) {
        if (logger.isDebugEnabled()) {
            logger.debug(getMessagePrepend() + format, argArray);
        }
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(getMessagePrepend() + msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(getMessagePrepend(), t);
        }
    }

    @Override
    public void info(final String msg) {
        if (logger.isInfoEnabled()) {
            logger.info(getMessagePrepend() + msg);
        }
    }

    @Override
    public void info(final String format, final Object arg) {
        if (logger.isInfoEnabled()) {
            logger.info(getMessagePrepend() + format, arg);
        }
    }

    @Override
    public void info(final String format, final Object argA, final Object argB) {
        if (logger.isInfoEnabled()) {
            logger.info(getMessagePrepend() + format, argA, argB);
        }
    }

    @Override
    public void info(final String format, final Object... argArray) {
        if (logger.isInfoEnabled()) {
            logger.info(getMessagePrepend() + format, argArray);
        }
    }

    @Override
    public void info(final String msg, final Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(getMessagePrepend() + msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn(getMessagePrepend(), t);
        }
    }

    @Override
    public void warn(final String msg) {
        if (logger.isWarnEnabled()) {
            logger.warn(getMessagePrepend() + msg);
        }
    }

    @Override
    public void warn(final String format, final Object arg) {
        if (logger.isWarnEnabled()) {
            logger.warn(getMessagePrepend() + format, arg);
        }
    }

    @Override
    public void warn(final String format, final Object... argArray) {
        if (logger.isWarnEnabled()) {
            logger.warn(getMessagePrepend() + format, argArray);
        }
    }

    @Override
    public void warn(final String format, final Object argA, final Object argB) {
        if (logger.isWarnEnabled()) {
            logger.warn(getMessagePrepend() + format, argA, argB);
        }
    }

    @Override
    public void warn(final String msg, final Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn(getMessagePrepend() + msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(Throwable t) {
        logger.error(getMessagePrepend(), t);
    }

    @Override
    public void error(final String msg) {
        logger.error(getMessagePrepend() + msg);
    }

    @Override
    public void error(final String format, final Object arg) {
        logger.error(getMessagePrepend() + format, arg);
    }

    @Override
    public void error(final String format, final Object argA, final Object argB) {
        logger.error(getMessagePrepend() + format, argA, argB);
    }

    @Override
    public void error(final String format, final Object... argArray) {
        logger.error(getMessagePrepend() + format, argArray);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        logger.error(getMessagePrepend() + msg, t);
    }
}
