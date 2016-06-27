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
package fr.gouv.vitam.ingest.web;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.MultipartConfigElement;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.core.exception.IngestException;
import fr.gouv.vitam.ingest.util.PropertyUtil;
import fr.gouv.vitam.ingest.web.servlet.UploadSipServlet;

public class IngestWebApp {

    private static VitamLogger VITAM_LOGGER = VitamLoggerFactory.getInstance(IngestWebApp.class);

    private static String PROPERTIES_FILE = "ingest-web.properties";
    private static String INGEST_WEB_PORT = "ingest.web.port";
    private static String INGEST_WEB_CONTEXT = "ingest.web.context";
    private static String INGEST_WEB_PORT_DEFAULT = "8082";
    private static String INGEST_WEB_CONTEXT_DEFAULT = "ingest-web";
    private static final String INGEST_MODULE_DIR = "ingest-web";
    private static Properties properties = null;

    public static void main(String[] args) throws Exception {

        if (properties == null) {
            try {
                properties = PropertyUtil.loadProperties(PROPERTIES_FILE, INGEST_MODULE_DIR);
            } catch (final IOException e) {
                VITAM_LOGGER.error(e.getMessage());
                throw new IngestException("properties error");
            }
        }

        final String port = properties != null ? (String) properties.get(INGEST_WEB_PORT) : INGEST_WEB_PORT_DEFAULT;
        final int iPort = Integer.parseInt(port == null ? INGEST_WEB_PORT_DEFAULT : port);
        final Server server = new Server(iPort);

        final String webContext = properties!=null ? properties.getProperty(INGEST_WEB_CONTEXT) : INGEST_WEB_CONTEXT_DEFAULT;
        final String contextWeb = !StringUtils.isBlank(webContext) ? webContext : INGEST_WEB_CONTEXT_DEFAULT;

        final WebAppContext webcontext = new WebAppContext();
        webcontext.setContextPath("/" + contextWeb);
        webcontext.setDescriptor("webapp/WEB-INF/web.xml");
        final URL webAppDir = Thread.currentThread().getContextClassLoader().getResource("webapp");
        webcontext.setResourceBase(webAppDir.toURI().toString());
        webcontext.setParentLoaderPriority(true);

        final ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/servlet");
        final ServletHolder servletHolder = new ServletHolder(UploadSipServlet.class);
        servletHolder.getRegistration().setMultipartConfig(
            new MultipartConfigElement("vitam/data/ingest-web", 1048576, 1048576, 262144));
        servletContextHandler.addServlet(servletHolder, "/upload");

        final HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[] {webcontext, servletContextHandler});
        server.setHandler(handlerList);

        server.start();
        server.join();
    }
}
