/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.performance;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * simple logger to use same logger with same format for all performance time.
 * To enable it, add
 * <logger name="fr.gouv.vitam.common.performance.PerformanceLogger" level="DEBUG" />
 * on logback.xml file
 */
public class PerformanceLogger {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PerformanceLogger.class);

    private static PerformanceLogger performanceLogger = new PerformanceLogger();

    /**
     * default constructor, cannot be instantiate.
     */
    private PerformanceLogger() {}

    /**
     * log performance information with step, action and task.
     *
     * @param step name of the step
     * @param action name of the action
     * @param task optional, use to log a treatment or a part of treatment
     * @param time duration of the treatment
     */
    public void log(String step, String action, String task, long time) {
        LOGGER.debug("{},{},{},{}", step, action, task, time);
    }

    /**
     * @param step
     * @param action
     * @param task
     * @param size of object
     * @param time
     */
    public void log(String step, String action, String task, long size, long time) {
        LOGGER.debug("{},{},{},{},{}", step, action, task, size, time);
    }

    /**
     * log performance information with step and action.
     *
     * @param step name of the step
     * @param action name of the action
     * @param time duration of the treatment
     */
    public void log(String step, String action, long time) {
        LOGGER.debug("{},{},{},{}", step, action, "", time);
    }

    /**
     * log performance information with step.
     *
     * @param step name of the step
     * @param time duration of the treatment
     */
    public void log(String step, long time) {
        LOGGER.debug("{},{},{},{}", step, "", "", time);
    }

    /**
     * @return single instance on {@link PerformanceLogger}
     */
    public static PerformanceLogger getInstance() {
        return performanceLogger;
    }
}
