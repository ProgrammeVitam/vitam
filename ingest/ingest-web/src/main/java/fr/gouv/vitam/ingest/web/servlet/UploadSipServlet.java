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
package fr.gouv.vitam.ingest.web.servlet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.core.exception.IngestException;
import fr.gouv.vitam.ingest.util.PropertyUtil;

/**
 * Created by bsui on 01/06/16.
 */
@WebServlet(name = "UploadSipServlet", urlPatterns = {"/upload"})
@MultipartConfig
public class UploadSipServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 124775838223922594L;

    private static VitamLogger VITAM_LOGGER = VitamLoggerFactory.getInstance(UploadSipServlet.class);

    private static final String PROPERTIES_FILE = "ingest-web.properties";
    private static final String INGEST_WEB_UPLOAD_DIR = "ingest.web.upload.dir";
    private static final String INGEST_MODULE_DIR = "ingest-web";


    private final int maxFileSize = 1000000 * 1024;
    private final int maxMemSize = 4 * 1024;
    private File file;
    private String filePath = "";
    private Properties properties;

    public UploadSipServlet() throws IngestException {

        if (properties == null) {
            try {
                properties = PropertyUtil.loadProperties(PROPERTIES_FILE, INGEST_MODULE_DIR);
            } catch (final IOException e) {
                VITAM_LOGGER.error(e.getMessage());
                throw new IngestException("loading properties ingest-web.properties failed");
            }
        }

        filePath = properties.getProperty(INGEST_WEB_UPLOAD_DIR);
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Check that we have a file upload request
        final boolean isMultipart = ServletFileUpload.isMultipartContent(request);

        response.setContentType("text/html");
        final java.io.PrintWriter out = response.getWriter();
        if (!isMultipart) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet SIP upload</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<p>No file uploaded</p>");
            out.println("</body>");
            out.println("</html>");
            return;
        }
        final DiskFileItemFactory factory = new DiskFileItemFactory();
        // maximum size that will be stored in memory
        factory.setSizeThreshold(maxMemSize);
        // Location to save data that is larger than maxMemSize.
        final File fileProperties = new File(properties.getProperty("ingest.web.upload.dir"));
        factory.setRepository(fileProperties);

        // Create a new file upload handler
        final ServletFileUpload upload = new ServletFileUpload(factory);
        // maximum file size to be uploaded.
        upload.setSizeMax(maxFileSize);

        try {
            // Parse the request to get file items.
            final List fileItems = upload.parseRequest(request);

            // Process the uploaded file items
            final Iterator i = fileItems.iterator();

            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet sip upload</title>");
            out.println("</head>");
            out.println("<body>");
            while (i.hasNext()) {
                final FileItem fi = (FileItem) i.next();
                if (!fi.isFormField()) {
                    // Get the uploaded file parameters
                    final String fieldName = fi.getFieldName();
                    final String fileName = fi.getName();
                    final String contentType = fi.getContentType();
                    final boolean isInMemory = fi.isInMemory();
                    final long sizeInBytes = fi.getSize();
                    // Write the file
                    // FIXME REVIEW So you write the file in the WebApp !!! Why ???

                    if (fileName.lastIndexOf("\\") >= 0) {
                        file = new File(filePath + "/" +
                            fileName.substring(fileName.lastIndexOf("\\")));
                    } else {
                        file = new File(filePath + "/" +
                            fileName.substring(fileName.lastIndexOf("\\") + 1));
                    }
                    fi.write(file);

                    processUploadSip(file);

                    out.println("Uploaded Filename: " + fileName + "<br>");
                }
            }
            out.println("</body>");
            out.println("</html>");
        } catch (final Exception ex) {
            VITAM_LOGGER.error("error upload sip", ex);
        }
    }

    private Response processUploadSip(File file) throws URISyntaxException {
        final Client clientUpload = ClientBuilder.newBuilder()
            .register(MultiPartFeature.class)
            .build();

        final WebTarget webTargetUpload =
            clientUpload.target(new URI(properties.getProperty("ingest.web.core.upload.url")));

        // FIXME REVIEW Probably not Multipart ! since only one argument (File)

        final MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

        final FileDataBodyPart filePart = new FileDataBodyPart("file", file, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        multiPart.bodyPart(filePart);

        final Invocation.Builder builder = webTargetUpload
            .request(MediaType.APPLICATION_JSON_TYPE);
        final Entity entity = Entity.entity(multiPart, multiPart.getMediaType());
        final Response response = builder.post(entity);

        return response;
    }


}
