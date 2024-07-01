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
package fr.gouv.vitam.collect.internal;

import fr.gouv.vitam.collect.internal.core.configuration.CollectInternalConfiguration;
import fr.gouv.vitam.collect.internal.server.BusinessApplication;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.resources.VitamServiceRegistry;
import fr.gouv.vitam.common.serverv2.VitamStarter;
import fr.gouv.vitam.common.serverv2.application.AdminApplication;

/**
 * launch collect server
 */
public class CollectInternalMain {

    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.collect-internal.port";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectInternalMain.class);
    private static final String CONF_FILE_NAME = "collect-internal.conf";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();
    private final VitamStarter vitamStarter;

    public CollectInternalMain(String configurationFile) {
        ParametersChecker.checkParameter(
            String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME),
            configurationFile
        );
        vitamStarter = new VitamStarter(
            CollectInternalConfiguration.class,
            configurationFile,
            BusinessApplication.class,
            AdminApplication.class
        );
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            LOGGER.error(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
            throw new IllegalArgumentException(
                String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME)
            );
        }

        CollectInternalMain collectInternalMain = new CollectInternalMain(args[0]);

        try {
            VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();
            serviceRegistry.checkDependencies(VitamConfiguration.getRetryNumber(), VitamConfiguration.getRetryDelay());
            collectInternalMain.startAndJoin();
        } catch (VitamApplicationServerException | InterruptedException e) {
            LOGGER.error(
                String.format(fr.gouv.vitam.common.server.VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) +
                e.getMessage(),
                e
            );
            try {
                collectInternalMain.stop();
            } catch (VitamApplicationServerException ex) {
                LOGGER.error("Error when trying to stop Collect application: \n {}", e);
                ex.printStackTrace();
            }
            System.exit(1);
        }
    }

    public void start() throws VitamApplicationServerException {
        vitamStarter.start();
    }

    public void startAndJoin() throws VitamApplicationServerException {
        vitamStarter.run();
    }

    public void stop() throws VitamApplicationServerException {
        vitamStarter.stop();
    }
}
