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
package fr.gouv.vitam.ingest.external.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.resources.AdminStatusResource;
import fr.gouv.vitam.common.server.application.resources.VitamServiceRegistry;
import fr.gouv.vitam.common.tenant.filter.TenantFilter;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;

/**
 * Ingest External web application
 */
public final class IngestExternalApplication
    extends AbstractVitamApplication<IngestExternalApplication, IngestExternalConfiguration> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalApplication.class);
    private static final String CONF_FILE_NAME = "ingest-external.conf";
    private static final String SHIRO_FILE = "shiro.ini";
    private static final String MODULE_NAME = "ingest-external";

    static VitamServiceRegistry serviceRegistry = null;


    /**
     * Ingest External constructor
     *
     * @param configuration
     */
    public IngestExternalApplication(String configuration) {
        super(IngestExternalConfiguration.class, configuration);
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
            final IngestExternalApplication application = new IngestExternalApplication(args[0]);
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

    @Override
    protected void setFilter(ServletContextHandler context, boolean isAdminConnector) throws VitamApplicationServerException {
        if (getConfiguration().isAuthentication()) {

            File shiroFile = null;
            try {
                shiroFile = PropertiesUtils.findFile(SHIRO_FILE);
            } catch (final FileNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                throw new VitamApplicationServerException(e.getMessage());
            }
            context.setInitParameter("shiroConfigLocations", "file:" + shiroFile.getAbsolutePath());
            context.addEventListener(new EnvironmentLoaderListener());
            context.addFilter(ShiroFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }

        // Tenant Filter
        try {
            JsonNode node = JsonHandler.toJsonNode(getConfiguration().getTenants());
            context.setInitParameter(GlobalDataRest.TENANT_LIST, JsonHandler.unprettyPrint(node));
            context.addFilter(TenantFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e.getMessage(), e);
            throw new VitamApplicationServerException(e.getMessage());
        }
    }

    private static void setServiceRegistry(VitamServiceRegistry newServiceRegistry) {
        serviceRegistry = newServiceRegistry;
    }

    @Override
    protected void registerInResourceConfig(ResourceConfig resourceConfig) {
        setServiceRegistry(new VitamServiceRegistry());
        final IngestExternalResource resource = new IngestExternalResource(getConfiguration());
        serviceRegistry.register(IngestInternalClientFactory.getInstance());
        // FIXME P1 Siegfried missing but different configuration...
        resourceConfig.register(resource)
            .register(SanityDynamicFeature.class)
            .register(SanityCheckerCommonFilter.class);
    }

    @Override
    protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AdminStatusResource(serviceRegistry));
        return true;
    }

}
