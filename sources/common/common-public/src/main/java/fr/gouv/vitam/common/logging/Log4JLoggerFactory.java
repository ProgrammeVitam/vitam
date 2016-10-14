/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Logger factory which creates an <a href="http://logging.apache.org/log4j/1.2/index.html">Apache Log4J</a> logger.
 * <br>
 * Inspired from Netty
 */
public final class Log4JLoggerFactory extends VitamLoggerFactory {

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
        final Logger logger = Logger.getRootLogger(); // NOSONAR keep it non static
        loggerSetLevel(logger, level);
    }

    static VitamLogLevel loggerGetLevel(final Logger logger) {
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

    @Override
    protected VitamLogLevel getLevelSpecific() {
        final Logger logger = Logger.getRootLogger(); // NOSONAR keep it non static
        return loggerGetLevel(logger);
    }

}
