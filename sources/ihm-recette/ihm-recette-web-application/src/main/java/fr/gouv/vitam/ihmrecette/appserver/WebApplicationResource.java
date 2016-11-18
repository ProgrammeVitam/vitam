/**
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
 */
package fr.gouv.vitam.ihmrecette.appserver;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server2.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ihmrecette.soapui.SoapUiClient;
import fr.gouv.vitam.ihmrecette.soapui.SoapUiClientFactory;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Web Application Resource class
 */
@Path("/v1/api")
public class WebApplicationResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResource.class);
    private final WebApplicationConfig webApplicationConfig;

    /**
     * Constructor
     * 
     * @param webApplicationConfig
     */
    public WebApplicationResource(WebApplicationConfig webApplicationConfig) {
        super(new BasicVitamStatusServiceImpl());
        LOGGER.debug("init Admin Management Resource server");
        this.webApplicationConfig = webApplicationConfig;
    }

    /**
     * Retrieve all the messages for logbook 
     * @return Response
     */
    @GET
    @Path("/messages/logbook")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookMessages() {
        // TODO P0 : If translation key could be the same in different .properties file, MUST add an unique prefix per file
        return Response.status(Status.OK).entity(VitamLogbookMessages.getAllMessages()).build();
    }

    /**
     * @param object user credentials
     * @return Response OK if login success
     */
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(JsonNode object) {
        final Subject subject = ThreadContext.getSubject();
        final String username = object.get("token").get("principal").textValue();
        final String password = object.get("token").get("credentials").textValue();

        if (username == null || password == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        final UsernamePasswordToken token = new UsernamePasswordToken(username, password);

        try {
            subject.login(token);
            // TODO P1 add access log
            LOGGER.info("Login success: " + username);
        } catch (final Exception uae) {
            LOGGER.debug("Login fail: " + username);
            return Response.status(Status.UNAUTHORIZED).build();
        }

        return Response.status(Status.OK).build();
    }

    /**
     * Generates the logbook operation statistics file (cvs format) relative to the operation parameter
     *
     * @param operationId logbook oeration id
     * @return the statistics file (csv format)
     */
    @GET
    @Path("/stat/{id_op}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getLogbookStatistics(@PathParam("id_op") String operationId) {
        try {
            final RequestResponse logbookOperationResult = UserInterfaceTransactionManager.selectOperationbyId(operationId);
            if (logbookOperationResult != null && logbookOperationResult.toJsonNode().has("$results")) {
                final JsonNode logbookOperation = logbookOperationResult.toJsonNode().get("$results");
                // Create csv file
                final ByteArrayOutputStream csvOutputStream =
                    JsonTransformer.buildLogbookStatCsvFile(logbookOperation);
                final byte[] csvOutArray = csvOutputStream.toByteArray();
                final ResponseBuilder response = Response.ok(csvOutArray);
                response.header("Content-Disposition", "attachment;filename=rapport.csv");
                response.header("Content-Length", csvOutArray.length);

                return response.build();
            }

            return Response.status(Status.NOT_FOUND).build();
        } catch (final LogbookClientException e) {
            LOGGER.error("Logbook Client NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Uploads the given file and returns the logbook operation id
     *
     * @param fileName the file name
     * @return the logbook operation id
     */
    @GET
    @Path("/upload/{file_name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFileFromServer(@PathParam("file_name") String fileName) {
        ParametersChecker.checkParameter("SIP path is a mandatory parameter", fileName);
        if (webApplicationConfig == null || webApplicationConfig.getSipDirectory() == null) {
            LOGGER.error("SIP directory not configured");
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("SIP directory not configured")
                .build();
        }

        // Read the selected file into an InputStream
        try (
            InputStream sipInputStream = new FileInputStream(webApplicationConfig.getSipDirectory() + "/" + fileName);
            IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            final Response response = client.upload(sipInputStream);
            final String ingestOperationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            return Response.status(response.getStatus()).entity(ingestOperationId).build();
        } catch (final VitamException e) {
            LOGGER.error("IngestExternalException in Upload sip", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
                .build();
        } catch (FileNotFoundException e) {
            LOGGER.error("The selected file is not found", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
        } catch (IOException e) {
            LOGGER.error("Error occured when trying to close the stream", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Launch soap UI test
     * 
     * @return the response status (no entity)
     */
    @GET
    @Path("/soapui/launch")
    public Response launchSoapUiTests() {
        SoapUiClient soapUi = SoapUiClientFactory.getInstance().getClient();
        
        try {
            soapUi.launchTests();
        } catch (FileNotFoundException e) {
            LOGGER.error("Soap ui script description file not found", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            LOGGER.error("Can not read SOAP-UI script input file or write report", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            LOGGER.error("Error while SOAP UI script execution", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).build();
    }

    /**
     * get last SOAP-UI tests results as Json Node
     * 
     * @return the result as json if Status is OK.
     */
    @GET
    @Path("/soapui/result")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSoapUiTestsResults() {
        SoapUiClient soapUi = SoapUiClientFactory.getInstance().getClient();
        JsonNode result = null;
        
        try {
            result = soapUi.getLastTestReport();
        } catch (InvalidParseOperationException e) {
            LOGGER.error("The reporting json can't be create", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
		}
        return Response.status(Status.OK).entity(result).build();
    }

}
