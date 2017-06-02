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

package fr.gouv.vitam.functional.administration.rest;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.resources.AdminStatusResource;
import fr.gouv.vitam.common.server.application.resources.VitamServiceRegistry;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import static java.lang.String.format;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Admin management web application
 */
public class AdminManagementApplication
    extends AbstractVitamApplication<AdminManagementApplication, AdminManagementConfiguration> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementApplication.class);
    private static final String CONF_FILE_NAME = "functional-administration.conf";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();

    static VitamServiceRegistry serviceRegistry = null;

    /**
     * AdminManagementApplication constructor
     *
     * @param configuration the server configuration
     */
    public AdminManagementApplication(String configuration) {
        super(AdminManagementConfiguration.class, configuration);
    }

    /**
     * Main method to run the APPLICATION (doing start and join)
     *
     * @param args command line parameters
     * @throws IllegalStateException when cannot start server
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    CONF_FILE_NAME));
            }
            final AdminManagementApplication application = new AdminManagementApplication(args[0]);
            if (serviceRegistry == null) {
                LOGGER.error("ServiceRegistry is not allocated");
                System.exit(1);
            }
            serviceRegistry.checkDependencies(VitamConfiguration.getRetryNumber(), VitamConfiguration.getRetryDelay());
            application.run();
        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void setServiceRegistry(VitamServiceRegistry newServiceRegistry) {
        serviceRegistry = newServiceRegistry;
    }

    @Override
    protected void registerInResourceConfig(ResourceConfig resourceConfig)  {
        try {

            AdminManagementConfiguration configuration = getConfiguration();
            setServiceRegistry(new VitamServiceRegistry());
            final AdminManagementResource resource = new AdminManagementResource(configuration);

            serviceRegistry
                .register(LogbookOperationsClientFactory.getInstance())
                .register(resource.getLogbookDbAccess());
            // TODO: 5/12/17 dependency to workspace, metadata, storage

            final MongoDbAccessAdminImpl mongoDbAccess = resource.getLogbookDbAccess();
            final VitamCounterService vitamCounterService =
                new VitamCounterService(mongoDbAccess, configuration.getTenants());

            final ProfileResource profileResource = new ProfileResource(getConfiguration(), mongoDbAccess,vitamCounterService);
            resourceConfig
                .register(resource)
                .register(new ContractResource(mongoDbAccess, vitamCounterService))
                .register(new ContextResource(mongoDbAccess, vitamCounterService))
                .register(profileResource);
        } catch (Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AdminStatusResource(serviceRegistry));
        return true;

    }
}
