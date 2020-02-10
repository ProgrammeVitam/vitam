/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import java.util.logging.Logger;

/**
 * Logger factory which creates a <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/logging/">java
 * .util.logging</a> logger. <br>
 * Inspired from Netty
 */
public final class JdkLoggerFactory extends VitamLoggerFactory {

    /**
     * @param level the vitam log level
     */
    public JdkLoggerFactory(final VitamLogLevel level) {
        super(level);
        seLevelSpecific(currentLevel);
    }

    @Override
    public VitamLogger newInstance(final String name) {
        final Logger logger = Logger.getLogger(name); // NOSONAR keep it non static
        // Note: JDK Logger does not allow level < INFO per global
        if (currentLevel == VitamLogLevel.DEBUG || currentLevel == VitamLogLevel.TRACE) {
            loggerSetLevel(logger, currentLevel);
        }
        return new JdkLogger(logger);
    }

    static void loggerSetLevel(final Logger logger, final VitamLogLevel level) {
        Level jdklevel;
        switch (level) {
            case TRACE:
                jdklevel = Level.FINEST;
                break;
            case DEBUG:
                jdklevel = Level.FINE;
                break;
            case INFO:
                jdklevel = Level.INFO;
                break;
            case WARN:
                jdklevel = Level.WARNING;
                break;
            case ERROR:
                jdklevel = Level.SEVERE;
                break;
            default:
                jdklevel = Level.WARNING;
                break;
        }
        logger.setLevel(jdklevel);
    }

    @Override
    protected void seLevelSpecific(final VitamLogLevel level) {
        final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // NOSONAR keep it non static
        loggerSetLevel(logger, level);
    }

    static VitamLogLevel loggerGetLevel(final Logger logger) {
        if (logger.isLoggable(Level.FINEST)) {
            return VitamLogLevel.TRACE;
        } else if (logger.isLoggable(Level.FINE)) {
            return VitamLogLevel.DEBUG;
        } else if (logger.isLoggable(Level.INFO)) {
            return VitamLogLevel.INFO;
        } else if (logger.isLoggable(Level.WARNING)) {
            return VitamLogLevel.WARN;
        } else if (logger.isLoggable(Level.SEVERE)) {
            return VitamLogLevel.ERROR;
        }
        return null;
    }

    @Override
    protected VitamLogLevel getLevelSpecific() {
        final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // NOSONAR keep it non static
        return loggerGetLevel(logger);
    }
}
