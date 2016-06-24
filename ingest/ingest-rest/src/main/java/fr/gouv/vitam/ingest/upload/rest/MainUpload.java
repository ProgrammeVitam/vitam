/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ingest.upload.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;

/**
 *
 * Class that runs application server
 *
 */
public class MainUpload {

    private static VitamLogger VITAM_LOGGER = VitamLoggerFactory.getInstance(MainUpload.class);
    private static Properties properties = null;
    private static Server server;
    private static final String PROPERTIES_FILE = "ingest-rest.properties";

    /**
     * main method that runs a server
     * 
     * @param args
     * @throws IOException
     * @throws VitamException
     */
    // FIXME REVIEW Use same logic than Logbook
    // TODO Peut être intéressant d'imprimer la stack trace pour avoir l'information complète de l'erreur avant exit ?
    public static void main(String[] args) throws IOException, VitamException {

        MainUpload.serverInitialisation(args);
        final String port = properties != null ? properties.getProperty("ingest.core.port")
            : Integer.toString(VitamServerFactory.getDefaultPort());

        try {
            MainUpload.serverStarting(Integer.parseInt(port));
            MainUpload.serverJoin();

        } catch (final Exception e) {
            VITAM_LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    protected static void serverInitialisation(String[] args) throws VitamException {
        if (properties == null) {
            try {
                File file;
                if (args.length > 1) {
                    file = new File(args[0]);
                } else {
                    file = PropertiesUtils.fileFromConfigFolder(PROPERTIES_FILE);
                }

                final FileInputStream fis = new FileInputStream(file);
                properties = new Properties();
                properties.load(fis);
            } catch (final IOException e) {
                VITAM_LOGGER.error(e.getMessage());
                throw new VitamException("loading properties ingest-rest.properties failed");
            }
        }

    }

    /**
     * runs a server
     * 
     * @param serverPort
     * @throws Exception
     */
    protected static void serverStarting(int port) throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(MOXyJsonProvider.class);
        resourceConfig.register(new UploadServiceImpl(properties));
        resourceConfig.packages("fr.gouv.vitam.ingest.upload.rest");

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server = new Server(port);
        server.setHandler(context);
        server.start();
    }



    protected static void serverJoin() throws InterruptedException {
        server.join();
    }



    /**
     * getter for server
     * 
     * @return Server
     */
    public static Server getServer() {
        return server;
    }

    /**
     * stops a started server
     * 
     * @throws Exception
     */
    public static void stopServer() throws Exception {
        server.stop();
    }


}
