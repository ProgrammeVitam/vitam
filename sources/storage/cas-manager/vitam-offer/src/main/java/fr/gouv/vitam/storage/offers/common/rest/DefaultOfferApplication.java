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

package fr.gouv.vitam.storage.offers.common.rest;

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.resources.AdminStatusResource;
import fr.gouv.vitam.common.storage.StorageConfiguration;

/**
 * Workspace offer web application
 */
public final class DefaultOfferApplication extends AbstractVitamApplication<DefaultOfferApplication, StorageConfiguration> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferApplication.class);
    private static final String WORKSPACE_CONF_FILE_NAME = "default-offer.conf";
    private static final String SHIRO_FILE = "shiro.ini";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();

    /**
     * LogbookApplication constructor
     *
     * @param configuration
     */
    protected DefaultOfferApplication(String configuration) {
        super(StorageConfiguration.class, configuration);
    }

    /**
     * LogbookApplication constructor
     *
     * @param configuration
     */
    public DefaultOfferApplication(StorageConfiguration configuration) {
        super(StorageConfiguration.class, configuration);
    }

    /**
     * Main method to run the application (doing start and join)
     *
     * @param args
     *            command line parameters
     * @throws IllegalStateException
     *             if the Vitam server cannot be launched
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, WORKSPACE_CONF_FILE_NAME));
                throw new IllegalArgumentException(
                        String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, WORKSPACE_CONF_FILE_NAME));
            }

            final DefaultOfferApplication application = new DefaultOfferApplication(args[0]);
            application.run();

        } catch (final Exception e) {
            LOGGER.error(format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    protected void registerInResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new DefaultOfferResource());
    }

    @Override
    protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AdminStatusResource());
        return true;
    }

    // FIXME Duplication of code with other components in TLS
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
            context.addFilter(ShiroFilter.class, "/*", EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST,
                    DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }
    }

}
