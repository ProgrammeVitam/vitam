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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * <a href="http://logging.apache.org/log4j/1.2/index.html">Apache Log4J</a> logger. <br>
 * Inspired from Netty
 */
final class Log4JLogger extends AbstractVitamLogger {
    // Since Logger, most of the exception are catch silently
    private static final long serialVersionUID = 2851357342488183058L;

    final transient Logger logger; // NOSONAR keep it non static

    /**
     * Following the pattern discussed in pages 162 through 168 of "The complete log4j manual".
     */
    static final String FQCN = Log4JLogger.class.getName();

    // Does the log4j version in use recognize the TRACE level?
    // The trace level was introduced in log4j 1.2.12.
    final boolean traceCapable;

    Log4JLogger(final Logger logger) {
        super(logger.getName());
        this.logger = logger;
        traceCapable = isTraceCapable();
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
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.log(FQCN, Level.INFO, TIME_TRACE_PREFIX + ft.getMessage(), ft.getThrowable());
        }
    }

    private boolean isTraceCapable() {
        try {
            logger.isTraceEnabled();
            return true;
        } catch (final NoSuchMethodError e) { // NOSONAR
            return false;
        }
    }

    /**
     * Is this logger instance enabled for the TRACE level?
     *
     * @return True if this Logger is enabled for level TRACE, false otherwise.
     */
    @Override
    public boolean isTraceEnabled() {
        if (traceCapable) {
            return logger.isTraceEnabled();
        } else {
            return logger.isDebugEnabled();
        }
    }

    /**
     * Log a message object at level TRACE.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void trace(final String msg) {
        logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, msg, null);
    }

    /**
     * Log a message at level TRACE according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for level TRACE.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void trace(final String format, final Object arg) {
        if (isTraceEnabled()) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft.getMessage(),
                ft.getThrowable());
        }
    }

    /**
     * Log a message at level TRACE according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the TRACE level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void trace(final String format, final Object argA, final Object argB) {
        if (isTraceEnabled()) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft.getMessage(),
                ft.getThrowable());
        }
    }

    /**
     * Log a message at level TRACE according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the TRACE level.
     * </p>
     *
     * @param format the format string
     * @param arguments an array of arguments
     */
    @Override
    public void trace(final String format, final Object... arguments) {
        if (isTraceEnabled()) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft.getMessage(),
                ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level TRACE with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, msg, t);
    }

    /**
     * Is this logger instance enabled for the DEBUG level?
     *
     * @return True if this Logger is enabled for level DEBUG, false otherwise.
     */
    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Log a message object at level DEBUG.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void debug(final String msg) {
        logger.log(FQCN, Level.DEBUG, msg, null);
    }

    /**
     * Log a message at level DEBUG according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for level DEBUG.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void debug(final String format, final Object arg) {
        if (logger.isDebugEnabled()) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level DEBUG according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the DEBUG level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void debug(final String format, final Object argA, final Object argB) {
        if (logger.isDebugEnabled()) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level DEBUG according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the DEBUG level.
     * </p>
     *
     * @param format the format string
     * @param arguments an array of arguments
     */
    @Override
    public void debug(final String format, final Object... arguments) {
        if (logger.isDebugEnabled()) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level DEBUG with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        logger.log(FQCN, Level.DEBUG, msg, t);
    }

    /**
     * Is this logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level, false otherwise.
     */
    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Log a message object at the INFO level.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void info(final String msg) {
        logger.log(FQCN, Level.INFO, msg, null);
    }

    /**
     * Log a message at level INFO according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the INFO level.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void info(final String format, final Object arg) {
        if (logger.isInfoEnabled()) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the INFO level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void info(final String format, final Object argA, final Object argB) {
        if (logger.isInfoEnabled()) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level INFO according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the INFO level.
     * </p>
     *
     * @param format the format string
     * @param argArray an array of arguments
     */
    @Override
    public void info(final String format, final Object... argArray) {
        if (logger.isInfoEnabled()) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void info(final String msg, final Throwable t) {
        logger.log(FQCN, Level.INFO, msg, t);
    }

    /**
     * Is this logger instance enabled for the WARN level?
     *
     * @return True if this Logger is enabled for the WARN level, false otherwise.
     */
    @Override
    public boolean isWarnEnabled() {
        return logger.isEnabledFor(Level.WARN);
    }

    /**
     * Log a message object at the WARN level.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void warn(final String msg) {
        logger.log(FQCN, Level.WARN, msg, null);
    }

    /**
     * Log a message at the WARN level according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the WARN level.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void warn(final String format, final Object arg) {
        if (logger.isEnabledFor(Level.WARN)) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the WARN level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the WARN level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void warn(final String format, final Object argA, final Object argB) {
        if (logger.isEnabledFor(Level.WARN)) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level WARN according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the WARN level.
     * </p>
     *
     * @param format the format string
     * @param argArray an array of arguments
     */
    @Override
    public void warn(final String format, final Object... argArray) {
        if (logger.isEnabledFor(Level.WARN)) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the WARN level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        logger.log(FQCN, Level.WARN, msg, t);
    }

    /**
     * Is this logger instance enabled for level ERROR?
     *
     * @return True if this Logger is enabled for level ERROR, false otherwise.
     */
    @Override
    public boolean isErrorEnabled() {
        return logger.isEnabledFor(Level.ERROR);
    }

    /**
     * Log a message object at the ERROR level.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void error(final String msg) {
        logger.log(FQCN, Level.ERROR, msg, null);
    }

    /**
     * Log a message at the ERROR level according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the ERROR level.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void error(final String format, final Object arg) {
        if (logger.isEnabledFor(Level.ERROR)) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the ERROR level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the ERROR level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void error(final String format, final Object argA, final Object argB) {
        if (logger.isEnabledFor(Level.ERROR)) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level ERROR according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the ERROR level.
     * </p>
     *
     * @param format the format string
     * @param argArray an array of arguments
     */
    @Override
    public void error(final String format, final Object... argArray) {
        if (logger.isEnabledFor(Level.ERROR)) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void error(final String msg, final Throwable t) {
        logger.log(FQCN, Level.ERROR, msg, t);
    }
}
