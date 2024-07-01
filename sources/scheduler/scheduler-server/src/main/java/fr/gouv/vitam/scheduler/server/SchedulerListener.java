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

package fr.gouv.vitam.scheduler.server;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileNotFoundException;

public class SchedulerListener implements ServletContextListener {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchedulerListener.class);
    private static SchedulerListener INSTANCE = null;
    private final Scheduler scheduler;

    private SchedulerListener() throws SchedulerException {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory(
                PropertiesUtils.getConfigFile("quartz.properties").getPath()
            );
            scheduler = schedulerFactory.getScheduler();
        } catch (FileNotFoundException e) {
            throw new SchedulerException(e);
        }
    }

    public static SchedulerListener getInstance() throws SchedulerException {
        if (INSTANCE == null) {
            INSTANCE = new SchedulerListener();
        }
        return INSTANCE;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.warn("Scheduler started");
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            LOGGER.error("cannot start scheduler", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.warn("Scheduler destroyed");
        try {
            scheduler.shutdown(true);
        } catch (SchedulerException e) {
            LOGGER.error("Cannot shutdown scheduler", e);
        }
    }
}
