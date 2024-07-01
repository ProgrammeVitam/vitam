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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * <a href= "http://java.sun.com/javase/6/docs/technotes/guides/logging/index.html">java. util.logging</a> logger. <br>
 * Inspired from Netty
 */
final class JdkLogger extends AbstractVitamLogger {

    private static final long serialVersionUID = -1767272577989225979L;

    static final String SELF = JdkLogger.class.getName();
    static final String SUPER = AbstractVitamLogger.class.getName();

    final transient Logger logger; // NOSONAR keep it non static

    JdkLogger(final Logger logger) {
        super(logger.getName());
        this.logger = logger;
    }

    @Override
    public void setLevel(VitamLogLevel level) {
        JdkLoggerFactory.loggerSetLevel(logger, level);
    }

    @Override
    public VitamLogLevel getLevel() {
        return JdkLoggerFactory.loggerGetLevel(logger);
    }

    @Override
    public void timeInfo(String msg) {
        if (isInfoEnabled()) {
            logger.info(TIME_TRACE_PREFIX + getMessagePrepend() + msg);
        }
    }

    @Override
    public void timeInfo(String format, Object... arguments) {
        if (isInfoEnabled()) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            log(SELF, Level.INFO, TIME_TRACE_PREFIX + ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Is this logger instance enabled for the FINEST level?
     *
     * @return True if this Logger is enabled for level FINEST, false otherwise.
     */
    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINEST);
    }

    /**
     * Log a message object at level FINEST.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void trace(final String msg) {
        if (logger.isLoggable(Level.FINEST)) {
            log(SELF, Level.FINEST, msg, null);
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for level FINEST.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void trace(final String format, final Object arg) {
        if (logger.isLoggable(Level.FINEST)) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the FINEST level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void trace(final String format, final Object argA, final Object argB) {
        if (logger.isLoggable(Level.FINEST)) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the FINEST level.
     * </p>
     *
     * @param format the format string
     * @param argArray an array of arguments
     */
    @Override
    public void trace(final String format, final Object... argArray) {
        if (logger.isLoggable(Level.FINEST)) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level FINEST with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        if (logger.isLoggable(Level.FINEST)) {
            log(SELF, Level.FINEST, msg, t);
        }
    }

    /**
     * Is this logger instance enabled for the FINE level?
     *
     * @return True if this Logger is enabled for level FINE, false otherwise.
     */
    @Override
    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    /**
     * Log a message object at level FINE.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void debug(final String msg) {
        if (logger.isLoggable(Level.FINE)) {
            log(SELF, Level.FINE, msg, null);
        }
    }

    /**
     * Log a message at level FINE according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for level FINE.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void debug(final String format, final Object arg) {
        if (logger.isLoggable(Level.FINE)) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level FINE according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the FINE level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void debug(final String format, final Object argA, final Object argB) {
        if (logger.isLoggable(Level.FINE)) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level FINE according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the FINE level.
     * </p>
     *
     * @param format the format string
     * @param argArray an array of arguments
     */
    @Override
    public void debug(final String format, final Object... argArray) {
        if (logger.isLoggable(Level.FINE)) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level FINE with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        if (logger.isLoggable(Level.FINE)) {
            log(SELF, Level.FINE, msg, t);
        }
    }

    /**
     * Is this logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level, false otherwise.
     */
    @Override
    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    /**
     * Log a message object at the INFO level.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void info(final String msg) {
        if (logger.isLoggable(Level.INFO)) {
            log(SELF, Level.INFO, msg, null);
        }
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
        if (logger.isLoggable(Level.INFO)) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
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
        if (logger.isLoggable(Level.INFO)) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
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
        if (logger.isLoggable(Level.INFO)) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
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
        if (logger.isLoggable(Level.INFO)) {
            log(SELF, Level.INFO, msg, t);
        }
    }

    /**
     * Is this logger instance enabled for the WARNING level?
     *
     * @return True if this Logger is enabled for the WARNING level, false otherwise.
     */
    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    /**
     * Log a message object at the WARNING level.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void warn(final String msg) {
        if (logger.isLoggable(Level.WARNING)) {
            log(SELF, Level.WARNING, msg, null);
        }
    }

    /**
     * Log a message at the WARNING level according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the WARNING level.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void warn(final String format, final Object arg) {
        if (logger.isLoggable(Level.WARNING)) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the WARNING level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the WARNING level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void warn(final String format, final Object argA, final Object argB) {
        if (logger.isLoggable(Level.WARNING)) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level WARNING according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the WARNING level.
     * </p>
     *
     * @param format the format string
     * @param argArray an array of arguments
     */
    @Override
    public void warn(final String format, final Object... argArray) {
        if (logger.isLoggable(Level.WARNING)) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the WARNING level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        if (logger.isLoggable(Level.WARNING)) {
            log(SELF, Level.WARNING, msg, t);
        }
    }

    /**
     * Is this logger instance enabled for level SEVERE?
     *
     * @return True if this Logger is enabled for level SEVERE, false otherwise.
     */
    @Override
    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    /**
     * Log a message object at the SEVERE level.
     *
     * @param msg - the message object to be logged
     */
    @Override
    public void error(final String msg) {
        if (logger.isLoggable(Level.SEVERE)) {
            log(SELF, Level.SEVERE, msg, null);
        }
    }

    /**
     * Log a message at the SEVERE level according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the SEVERE level.
     * </p>
     *
     * @param format the format string
     * @param arg the argument
     */
    @Override
    public void error(final String format, final Object arg) {
        if (logger.isLoggable(Level.SEVERE)) {
            final FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the SEVERE level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the SEVERE level.
     * </p>
     *
     * @param format the format string
     * @param argA the first argument
     * @param argB the second argument
     */
    @Override
    public void error(final String format, final Object argA, final Object argB) {
        if (logger.isLoggable(Level.SEVERE)) {
            final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level SEVERE according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the SEVERE level.
     * </p>
     *
     * @param format the format string
     * @param arguments an array of arguments
     */
    @Override
    public void error(final String format, final Object... arguments) {
        if (logger.isLoggable(Level.SEVERE)) {
            final FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the SEVERE level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    @Override
    public void error(final String msg, final Throwable t) {
        if (logger.isLoggable(Level.SEVERE)) {
            log(SELF, Level.SEVERE, msg, t);
        }
    }

    /**
     * Log the message at the specified level with the specified throwable if any. This method creates a LogRecord and
     * fills in caller date before calling this instance's JDK14 logger.
     *
     * See bug report #13 for more details.
     */
    private void log(final String callerFQCN, final Level level, final String msg, final Throwable t) {
        // millis and thread are filled by the constructor
        final LogRecord record = new LogRecord(level, msg);
        record.setLoggerName(name());
        record.setThrown(t);
        fillCallerData(callerFQCN, record);
        logger.log(record);
    }

    /**
     * Fill in caller data if possible.
     *
     * @param record The record to update
     */
    private static void fillCallerData(final String callerFQCN, final LogRecord record) {
        final StackTraceElement[] steArray = new Throwable().getStackTrace();

        int selfIndex = -1;
        for (int i = 0; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (className.equals(callerFQCN) || className.equals(SUPER)) {
                selfIndex = i;
                break;
            }
        }

        int found = -1;
        for (int i = selfIndex + 1; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (!(className.equals(callerFQCN) || className.equals(SUPER))) {
                found = i;
                break;
            }
        }

        if (found != -1) {
            final StackTraceElement ste = steArray[found];
            // setting the class name has the side effect of setting
            // the needToInferCaller variable to false.
            record.setSourceClassName(ste.getClassName());
            record.setSourceMethodName(ste.getMethodName());
        }
    }
}
