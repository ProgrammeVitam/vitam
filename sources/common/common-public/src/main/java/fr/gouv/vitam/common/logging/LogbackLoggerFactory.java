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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

import javax.management.RuntimeErrorException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * logger factory using SLF4J from Logback
 */
public final class LogbackLoggerFactory extends VitamLoggerFactory {

    // Since Logger, most of the exception are catch silently
    static final String ROOT = Logger.ROOT_LOGGER_NAME;

    /**
     * @param level the vitam log level
     */
    public LogbackLoggerFactory(final VitamLogLevel level) {
        super(level);
        seLevelSpecific(currentLevel);
    }

    LogbackLoggerFactory(final boolean failIfNOP) {
        super(null);
        // Should be always called with true.
        assert failIfNOP;

        // SFL4J writes it error messages to System.err. Capture them so that
        // the user does not see such a message on
        // the console during automatic detection.
        final StringBuilder buf = new StringBuilder();
        final PrintStream err = System.err; // NOSONAR
        try {
            System.setErr(
                new PrintStream(
                    new OutputStream() {
                        @Override
                        public void write(final int b) {
                            buf.append((char) b);
                        }
                    },
                    true,
                    "US-ASCII"
                )
            );
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeErrorException(new Error(e));
        }

        try {
            if (LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory) {
                throw new NoClassDefFoundError(buf.toString());
            } else {
                err.print(buf.toString());
                err.flush();
            }
        } finally {
            System.setErr(err);
            seLevelSpecific(currentLevel);
        }
    }

    static void loggerSetLevel(final Logger logger, final VitamLogLevel level) {
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
    protected void seLevelSpecific(final VitamLogLevel level) {
        final Logger logger = (Logger) LoggerFactory.getLogger(ROOT); // NOSONAR keep it non static
        loggerSetLevel(logger, level);
    }

    @Override
    public VitamLogger newInstance(final String name) {
        final Logger logger = (Logger) LoggerFactory.getLogger(name); // NOSONAR keep it non static
        return new LogbackLogger(logger);
    }

    static VitamLogLevel loggerGetLevel(Logger logger) {
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

    @Override
    protected VitamLogLevel getLevelSpecific() {
        final Logger logger = (Logger) LoggerFactory.getLogger(ROOT); // NOSONAR keep it non static
        return loggerGetLevel(logger);
    }
}
