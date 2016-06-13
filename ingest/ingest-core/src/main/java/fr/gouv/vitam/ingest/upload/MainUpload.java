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
package fr.gouv.vitam.ingest.upload;

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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.core.exception.IngestException;
import fr.gouv.vitam.ingest.util.PropertyUtil;

/**
 * Created by bsui on 17/05/16.
 */
public class MainUpload {

    private static VitamLogger VITAM_LOGGER = VitamLoggerFactory.getInstance(MainUpload.class);
    private static final String PROPERTIES_CORE = "ingest-core.properties";
    private static final String INGEST_MODULE_DIR = "ingest-core";
    private static Properties properties = null;
    private static Server server;

    public static void main(String[] args) throws IOException, IngestException {

        if (properties == null) {
            try {
                properties = PropertyUtil.loadProperties(UploadServiceImpl.PROPERTIES_CORE, INGEST_MODULE_DIR);
            } catch (final IOException e) {
                VITAM_LOGGER.error(e.getMessage());
                throw new IngestException("properties error");
            }
        }

        final String port = properties.getProperty("ingest.core.port");

        /*
         * ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
         * servletContext.setContextPath("/vitam"); ResourceConfig resourceConfig = new ResourceConfig();
         * resourceConfig.register(JacksonFeature.class); resourceConfig.register(MultiPartFeature.class);
         * resourceConfig.register(MOXyJsonProvider.class); resourceConfig.register(UploadServiceImpl.class);
         * resourceConfig.packages("fr.gouv.vitam.ingest.upload"); ServletContainer servletContainer = new
         * ServletContainer(resourceConfig); ServletHolder servletHolder = new ServletHolder(servletContainer);
         * servletHolder.setInitOrder(1); servletHolder.setInitParameter("jersey.config.server.provider.packages",
         * "fr.gouv.vitam.ingest.upload"); servletHolder.setInitParameter("javax.ws.rs.Application",
         * "fr.gouv.vitam.ingest.upload.UploadServiceImpl"); servletContext.addServlet(servletHolder, "/*");
         * ContextHandlerCollection contexts = new ContextHandlerCollection(); contexts.setHandlers(new Handler[] {
         * servletContext}); server.setHandler(contexts); try { server.start(); server.join(); } catch (Throwable t) {
         * VITAM_LOGGER.error(t.getMessage(), t); }
         */


        try {
            new MainUpload();
            MainUpload.run(Integer.parseInt(port));
            server.join();
        } catch (final Exception e) {
            VITAM_LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }



    public static void run(int serverPort) throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(MOXyJsonProvider.class);
        resourceConfig.register(new UploadServiceImpl());
        resourceConfig.packages("fr.gouv.vitam.ingest.upload");

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server = new Server(serverPort);
        server.setHandler(context);
        server.start();
    }

}
