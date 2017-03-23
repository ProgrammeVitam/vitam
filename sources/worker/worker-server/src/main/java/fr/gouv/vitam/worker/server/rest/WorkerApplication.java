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

package fr.gouv.vitam.worker.server.rest;

import java.io.FileNotFoundException;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.resources.AdminStatusResource;
import fr.gouv.vitam.common.server.application.resources.VitamServiceRegistry;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.PluginException;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.plugin.PluginLoader;
import fr.gouv.vitam.worker.server.registration.WorkerRegistrationListener;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Worker web application
 */
public final class WorkerApplication extends AbstractVitamApplication<WorkerApplication, WorkerConfiguration> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerApplication.class);
    private static final String CONF_FILE_NAME = "worker.conf";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();

    // Only for Junit TODO P2
    static Worker mock = null;
    static VitamServiceRegistry serviceRegistry = null;
    private PluginLoader pluginLoader;

    /**
     * WorkerApplication constructor
     *
     * @param configuration the configuration file
     * @throws FileNotFoundException if configuration file not found
     * @throws InvalidParseOperationException if invalid parser configuration file
     * @throws PluginException if plugin loading exception occurred
     */
    public WorkerApplication(String configuration)
        throws FileNotFoundException, PluginException, InvalidParseOperationException {
        super(WorkerConfiguration.class, configuration);

        // initialize in registerInResourceConfig()
        pluginLoader.loadConfiguration();
        checkPluginsCreation();
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
            final WorkerApplication application = new WorkerApplication(args[0]);
            // Test if dependencies are OK
            if (serviceRegistry == null) {
                LOGGER.error("ServiceRegistry is not allocated");
                System.exit(1);
            }
            serviceRegistry.checkDependencies(VitamConfiguration.getRetryNumber(), VitamConfiguration.getRetryDelay());

            application.run();
        } catch (final Exception e) {
            LOGGER.error(String.format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    private void checkPluginsCreation() throws FileNotFoundException, InvalidParseOperationException, PluginException {
        pluginLoader.loadAllPlugins();
    }

    @Override
    protected void setFilter(ServletContextHandler context) {
        context.addEventListener(new WorkerRegistrationListener(getConfiguration()));
    }

    private static void setServiceRegistry(VitamServiceRegistry newServiceRegistry) {
        serviceRegistry = newServiceRegistry;
    }

    @Override
    protected void registerInResourceConfig(ResourceConfig resourceConfig) {
        setServiceRegistry(new VitamServiceRegistry());

        // initialisation here because this method is call in the parent constructor
        pluginLoader = new PluginLoader();

        if (mock != null) {
            resourceConfig.register(new WorkerResource(pluginLoader, mock));
        } else {
            resourceConfig.register(new WorkerResource(pluginLoader));
            WorkspaceClientFactory.changeMode(getConfiguration().getUrlWorkspace());
            // Logbook dependency
            serviceRegistry.register(LogbookLifeCyclesClientFactory.getInstance())
                // Workspace dependency
                .register(WorkspaceClientFactory.getInstance())
                // Metadata dependency
                .register(MetaDataClientFactory.getInstance());
            // FIXME P1 Siegfried missing but different configuration...
            // Processing dependency: optional ?
            // serviceRegistry.register(ProcessingManagementClientFactory.getInstance());
        }
    }

    @Override
    protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AdminStatusResource(serviceRegistry));
        return true;
    }

}
