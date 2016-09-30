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

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;

/**
 * Ingest External web application
 */
public final class IngestExternalApplication extends AbstractVitamApplication<IngestExternalApplication, IngestExternalConfiguration> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalApplication.class);
    private static final String CONF_FILE_NAME = "ingest-external.conf";
    private static final String SHIRO_FILE = "shiro.ini";
    private static final String MODULE_NAME = "ingest-external";
    private static IngestExternalConfiguration serverConfiguration;

    /**
     * Ingest External constructor
     */
    protected IngestExternalApplication() {
        super(IngestExternalApplication.class, IngestExternalConfiguration.class);
    }

    /**
     * Main method to run the application (doing start and join)
     *
     * @param args command line parameters
     * @throws IllegalStateException
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    CONF_FILE_NAME));
            }

            final VitamServer vitamServer = startApplication(args[0]);
            vitamServer.run();
        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Prepare the application to be run or started.
     *
     * @param moduleConf moduleconf
     * @return the VitamServer
     * @throws IllegalStateException
     */
    public static VitamServer startApplication(String moduleConf) throws VitamException {

        final IngestExternalApplication application = new IngestExternalApplication();
        application.configure(application.computeConfigurationPathFromInputArguments(moduleConf));
        serverConfiguration = application.getConfiguration();

        LOGGER.info(format(VitamServer.SERVER_START_WITH_JETTY_CONFIG, MODULE_NAME));
        VitamServer vitamServer = VitamServerFactory.newVitamServerByJettyConf(serverConfiguration.getJettyConfig());
        vitamServer.configure(application.getApplicationHandler());

        return vitamServer;
    }
    
    @Override
    protected String getConfigFilename() {
        return CONF_FILE_NAME;
    }

    /**
     * Implement this method to construct your application specific handler
     *
     * @return the generated Handler
     * @throws VitamApplicationServerException 
     */
    @Override
    protected Handler buildApplicationHandler() throws VitamApplicationServerException {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new IngestExternalResource(getConfiguration()));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        
        if(getConfiguration().isAuthentication()) {
        
            File shiroFile=null;
            try {
                shiroFile = PropertiesUtils.findFile(SHIRO_FILE);
            } catch (FileNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                throw new VitamApplicationServerException(e.getMessage());
            }
            context.setInitParameter("shiroConfigLocations", "file:"+shiroFile.getAbsolutePath());
            context.addEventListener(new EnvironmentLoaderListener());
            context.addFilter(ShiroFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR));
        }
        
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        return context;
    }

}
