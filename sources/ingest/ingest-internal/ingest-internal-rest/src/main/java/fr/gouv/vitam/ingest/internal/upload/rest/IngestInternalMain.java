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
package fr.gouv.vitam.ingest.internal.upload.rest;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
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
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Ingest Internal web server application
 */
public class IngestInternalMain {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalMain.class);
    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.ingest-internal.port";

    private static final String CONF_FILE_NAME = "ingest-internal.conf";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();

    private VitamStarter vitamStarter;

    public IngestInternalMain(String configurationFile) {
        ParametersChecker.checkParameter(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
            CONF_FILE_NAME), configurationFile);
        vitamStarter = new VitamStarter(IngestInternalConfiguration.class, configurationFile,
            BusinessApplication.class, AdminApplication.class);
    }

    /**
     * Main method to run the application (doing start and join)
     *
     * @param args command line parameters
     * @throws IllegalStateException if the Vitam server cannot be launched
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    CONF_FILE_NAME));
            }
            IngestInternalMain main = new IngestInternalMain(args[0]);
            VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();
            try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(args[0])) {
                final IngestInternalConfiguration configuration =
                    PropertiesUtils.readYaml(yamlIS, IngestInternalConfiguration.class);
                WorkspaceClientFactory.changeMode(configuration.getWorkspaceUrl());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Register Workspace
            serviceRegistry.register(WorkspaceClientFactory.getInstance())
                // Register Logbook for Operation
                .register(LogbookOperationsClientFactory.getInstance())
                // Register Storage (ATR access)
                .register(StorageClientFactory.getInstance())
                // Register ProcessingManagement
                .register(ProcessingManagementClientFactory.getInstance());
            // Database dependency
            serviceRegistry.checkDependencies(VitamConfiguration.getRetryNumber(), VitamConfiguration.getRetryDelay());

            main.startAndJoin();
        } catch (Exception e) {
            LOGGER.error(String.format(fr.gouv.vitam.common.server.VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) +
                e.getMessage(), e);

            System.exit(1);
        }
    }

    /**
     * Start application
     *
     * @throws VitamApplicationServerException
     */
    public void start() throws VitamApplicationServerException {
        vitamStarter.start();
    }

    /**
     * Start and join application
     *
     * @throws VitamApplicationServerException
     */
    public void startAndJoin() throws VitamApplicationServerException {
        vitamStarter.run();
    }

    /**
     * Stop application
     *
     * @throws VitamApplicationServerException
     */
    public void stop() throws VitamApplicationServerException {
        vitamStarter.stop();
    }

    /**
     * Get the Vitam Starter
     *
     * @return
     */
    public VitamStarter getVitamStarter() {
        return vitamStarter;
    }

}
