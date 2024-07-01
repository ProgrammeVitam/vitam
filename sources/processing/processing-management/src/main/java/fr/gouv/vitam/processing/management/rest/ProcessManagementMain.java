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
package fr.gouv.vitam.processing.management.rest;

import com.google.common.annotations.VisibleForTesting;
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
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Application;

/**
 * The process management application is to launch process engine vitamServer
 */
public class ProcessManagementMain {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementMain.class);
    private static final String CONF_FILE_NAME = "processing.conf";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();
    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.processing.port";

    private final VitamStarter vitamStarter;

    public ProcessManagementMain(String configurationFile) {
        ParametersChecker.checkParameter(
            String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME),
            configurationFile
        );
        vitamStarter = new VitamStarter(
            ServerConfiguration.class,
            configurationFile,
            BusinessApplication.class,
            AdminApplication.class
        );
        VitamApplicationInitializr.get().initialize(configurationFile);
        vitamStarter
            .getVitamServer()
            .getServer()
            .addEventListener(VitamApplicationInitializr.get().getVitamServerLifeCycle());
    }

    @VisibleForTesting
    public ProcessManagementMain(
        String configurationFile,
        Class<? extends Application> businessApplication,
        Class<? extends Application> adminApplication
    ) {
        ParametersChecker.checkParameter(
            String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME),
            configurationFile
        );
        if (null == businessApplication) {
            businessApplication = BusinessApplication.class;
        }

        if (null == adminApplication) {
            adminApplication = AdminApplication.class;
        }
        vitamStarter = new VitamStarter(
            ServerConfiguration.class,
            configurationFile,
            businessApplication,
            adminApplication
        );
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(
                    String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME)
                );
            }
            ProcessManagementMain main = new ProcessManagementMain(args[0]);
            VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();
            serviceRegistry
                // Logbook dependency
                .register(LogbookOperationsClientFactory.getInstance())
                // Workspace dependency
                .register(WorkspaceClientFactory.getInstance())
                // Metadata dependency: optional ???
                .registerOptional(MetaDataClientFactory.getInstance());

            // Database dependency
            serviceRegistry.checkDependencies(VitamConfiguration.getRetryNumber(), VitamConfiguration.getRetryDelay());

            main.startAndJoin();
        } catch (Exception e) {
            LOGGER.error(
                String.format(fr.gouv.vitam.common.server.VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) +
                e.getMessage(),
                e
            );

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
