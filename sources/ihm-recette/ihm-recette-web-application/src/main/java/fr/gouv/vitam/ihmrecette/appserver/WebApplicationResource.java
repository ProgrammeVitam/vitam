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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import fr.gouv.vitam.ihmdemo.common.pagination.OffsetBasedPagination;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ihmrecette.soapui.SoapUiClient;
import fr.gouv.vitam.ihmrecette.soapui.SoapUiClientFactory;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

/**
 * Web Application Resource class
 */
@Path("/v1/api")
public class WebApplicationResource extends ApplicationStatusResource {

    private static final String FILENAME_REGEX = "^[a-z0-9_-]*(\\.)(zip|tar|tar.gz|tar.bz2)$";
    private static final Pattern FILENAME_PATTERN = Pattern.compile(FILENAME_REGEX, Pattern.CASE_INSENSITIVE);
    private static final String RESULTS_FIELD = "$results";
    private static final String FILE_NAME_KEY = "fileName";
    private static final String FILE_SIZE_KEY = "fileSize";
    private static final String ZIP_EXTENSION = ".ZIP";
    private static final String TAR_GZ_EXTENSION = ".TAR.GZ";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResource.class);
    public static final String IHM_RECETTE = "IHM_RECETTE";
    private static final String DEFAULT_TENANT = "0";
    private final WebApplicationConfig webApplicationConfig;

    // FIXME : replace the boolean by a static timestamp updated by the soap ui
    // thread
    private static volatile boolean soapUiRunning = false;

    protected static boolean isSoapUiRunning() {
        return soapUiRunning;
    }

    private static void setSoapUiRunning(boolean soapUiRunning) {
        WebApplicationResource.soapUiRunning = soapUiRunning;
    }

    // TODO FIX_TENANT_ID
    private static final Integer TENANT_ID = 0;

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
     *
     *
     * @return Response
     */
    @GET
    @Path("/messages/logbook")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookMessages() {
        // TODO P0 : If translation key could be the same in different
        // .properties file, MUST add an unique prefix per
        // file
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
     * Returns the list of available files
     *
     * @return the list of available files
     */
    @GET
    @Path("/upload/fileslist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableFilesList() {

        if (webApplicationConfig == null || webApplicationConfig.getSipDirectory() == null) {
            LOGGER.error("SIP directory not configured");
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("SIP directory not configured").build();
        }

        final File fileDirectory = new File(webApplicationConfig.getSipDirectory());

        if (!fileDirectory.isDirectory()) {
            LOGGER.error("SIP directory <{}> is not a directory.", webApplicationConfig.getSipDirectory());
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity("SIP directory [" + webApplicationConfig.getSipDirectory() + "] is not a directory")
                .build();
        }
        final File[] sipFiles = fileDirectory.listFiles(new SipFilenameFilterImpl());
        final ArrayNode filesListDetails = JsonHandler.createArrayNode();

        if (sipFiles != null) {
            for (final File currentFile : sipFiles) {

                if (FILENAME_PATTERN.matcher(currentFile.getName()).matches()) {
                    final ObjectNode fileDetails = JsonHandler.createObjectNode();
                    fileDetails.put(FILE_NAME_KEY, currentFile.getName());
                    fileDetails.put(FILE_SIZE_KEY, currentFile.length());
                    filesListDetails.add(fileDetails);
                } else {
                    LOGGER.warn("SIP filename incorrect {}", currentFile.getName());
                }
            }
        }

        return Response.status(Status.OK).entity(filesListDetails).build();
    }

    private class SipFilenameFilterImpl implements FilenameFilter {
        @Override
        public boolean accept(File dir, String fileName) {
            return fileName.toUpperCase().endsWith(ZIP_EXTENSION) || fileName.toUpperCase().endsWith(TAR_GZ_EXTENSION);
        }
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
        LOGGER.debug("/stat/id_op / id: " + operationId);
        try {
            final RequestResponse logbookOperationResult = UserInterfaceTransactionManager
                .selectOperationbyId(operationId, TENANT_ID);
            if (logbookOperationResult != null && logbookOperationResult.toJsonNode().has(RESULTS_FIELD)) {
                final JsonNode logbookOperation = ((ArrayNode) logbookOperationResult.toJsonNode().get(RESULTS_FIELD))
                    .get(0);
                // Create csv file
                final ByteArrayOutputStream csvOutputStream = JsonTransformer.buildLogbookStatCsvFile(logbookOperation);
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
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("SIP directory not configured").build();
        }

        if (!FILENAME_PATTERN.matcher(fileName).matches()) {
            LOGGER.error("SIP path  invalid");
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("SIP path  invalid").build();
        }

        // Read the selected file into an InputStream
        try (InputStream sipInputStream = new FileInputStream(webApplicationConfig.getSipDirectory() + "/" + fileName);
            IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            final Response response = client.upload(sipInputStream, TENANT_ID);
            final String ingestOperationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            return Response.status(response.getStatus()).entity(ingestOperationId).build();
        } catch (final VitamException e) {
            LOGGER.error("IngestExternalException in Upload sip", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
        } catch (final FileNotFoundException e) {
            LOGGER.error("The selected file is not found", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final IOException e) {
            LOGGER.error("Error occured when trying to close the stream", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Launch soap UI test
     *
     * @return the response status (no entity)
     */
    @GET
    @Path("/soapui/launch")
    public synchronized Response launchSoapUiTests() {
        if (!WebApplicationResource.isSoapUiRunning()) {
            WebApplicationResource.setSoapUiRunning(true);
            VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> soapUiAsync());
            return Response.status(Status.OK).build();
        } else {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * FIXME : use a better way to launch SOAP UI in another thread to manager responses
     */
    private void soapUiAsync() {
        final SoapUiClient soapUi = SoapUiClientFactory.getInstance().getClient();
        try {
            soapUi.launchTests();
        } catch (final FileNotFoundException e) {
            LOGGER.error("Soap ui script description file not found", e);
        } catch (final IOException e) {
            LOGGER.error("Can not read SOAP-UI script input file or write report", e);
        } catch (final InterruptedException e) {
            LOGGER.error("Error while SOAP UI script execution", e);
        }
        WebApplicationResource.setSoapUiRunning(false);
    }

    /**
     * Check if soap UI test is running
     *
     * @return the response status (no entity)
     */
    @GET
    @Path("/soapui/running")
    @Produces(MediaType.APPLICATION_JSON)
    public Response soapUiTestsRunning() {
        return Response.status(Status.OK)
            .entity(JsonHandler.createObjectNode().put("result", WebApplicationResource.soapUiRunning)).build();
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
        final SoapUiClient soapUi = SoapUiClientFactory.getInstance().getClient();
        JsonNode result = null;

        try {
            result = soapUi.getLastTestReport();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("The reporting json can't be create", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }



    /**
     *
     * @return
     * @throws LogbookClientServerException
     */
    @POST
    @Path("/operations/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceability() throws LogbookClientServerException {
        try (final LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            RequestResponseOK result;
            try {
                result = logbookOperationsClient.traceability();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error("The reporting json can't be created", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .build();
            }
            return Response.status(Status.OK).entity(result).build();
        }
    }

    /**
     *
     * Post used because Angular not support Get with body
     *
     * @param headers
     * @param sessionId
     * @param options
     * @return Response
     */
    @POST
    @Path("/logbooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResultByBrowser(@Context HttpHeaders headers,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @CookieParam("JSESSIONID") String sessionId,
        String options) {
        if (xhttpOverride == null || !"GET".equalsIgnoreCase(xhttpOverride)) {
            final Status status = Status.PRECONDITION_FAILED;
            VitamError vitamError = new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(
                IHM_RECETTE).setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase());
            return Response.status(status).entity(vitamError).build();
        }

        return findLogbookBy(headers, sessionId, options);
    }

    /**
     * this method is used to request logbook with the Vitam DSL
     *
     * @param headers header containing the pagination for logbook
     * @param sessionId using for pagination
     * @param options JSON object representing the Vitam DSL query
     * @return Response
     */
    @GET
    @Path("/logbooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResult(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
        String options) {
        return findLogbookBy(headers, sessionId, options);
    }

    private Response findLogbookBy(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
        String options) {
        ParametersChecker.checkParameter("cookie is mandatory", sessionId);
        String requestId = null;
        RequestResponse result = null;
        OffsetBasedPagination pagination = null;

        try {
            pagination = new OffsetBasedPagination(headers);
        } catch (final VitamException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        final List<String> requestIds = HttpHeaderHelper.getHeaderValues(headers, IhmWebAppHeader.REQUEST_ID.name());
        if (requestIds != null) {
            requestId = requestIds.get(0);
            // get result from shiro session
            try {
                result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(sessionId, pagination));

                return Response.status(Status.OK).entity(result).header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                    .header(IhmDataRest.X_LIMIT, pagination.getLimit()).build();
            } catch (final VitamException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
            }
        } else {
            requestId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

            try {
                ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
                final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);

                LOGGER.debug("query >>>>>>>>>>>>>>>>> : " + query);
                result = UserInterfaceTransactionManager.selectOperation(query, TENANT_ID);

                // save result
                LOGGER.debug("resultr <<<<<<<<<<<<<<<<<<<<<<<: " + result);
                PaginationHelper.setResult(sessionId, result.toJsonNode());
                // pagination
                result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(result.toJsonNode(), pagination));

            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
            } catch (final LogbookClientException e) {
                LOGGER.error("Logbook Client NOT FOUND Exception ", e);
                return Response.status(Status.NOT_FOUND).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
            } catch (final Exception e) {
                LOGGER.error("Internal server error", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .build();
            }
            return Response.status(Status.OK).entity(result).header(GlobalDataRest.X_REQUEST_ID, requestId)
                .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                .header(IhmDataRest.X_LIMIT, pagination.getLimit()).build();
        }
    }

    /**
     *
     * @param operationId id of operation
     * @return Response
     */
    @GET
    @Path("/logbooks/{idOperation}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResultById(@PathParam("idOperation") String operationId) {
        try {
            final RequestResponse<JsonNode> result =
                UserInterfaceTransactionManager.selectOperationbyId(operationId, TENANT_ID);
            return Response.status(Status.OK).entity(result).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final LogbookClientException e) {
            LOGGER.error("Logbook Client NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     *
     * @param operationId
     * @param asyncResponse
     */

    @GET
    @Path("/logbooks/{idOperation}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void downloadObjectAsStream(@PathParam("idOperation") String operationId,
        @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> downloadObjectAsync(asyncResponse, operationId));
    }

    /**
     * This method exist only to download a file with a browser
     *
     * @param operationId
     * @param asyncResponse
     */
    @GET
    @Path("/logbooks/{idOperation}/content")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void downloadObjectAsStreamForBrowser(@PathParam("idOperation") String operationId,
        @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> downloadObjectAsync(asyncResponse, operationId));
    }

    /**
     * download object for LOGBOOKS type
     *
     * @param asyncResponse
     * @param operationId
     */
    private void downloadObjectAsync(final AsyncResponse asyncResponse, String operationId) {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            final RequestResponse<JsonNode> result =
                UserInterfaceTransactionManager.selectOperationbyId(operationId, TENANT_ID);

            RequestResponseOK<JsonNode> responseOK = (RequestResponseOK<JsonNode>) result;
            List<JsonNode> results = responseOK.getResults();
            JsonNode operation = results.get(0);

            ArrayNode events = (ArrayNode) operation.get(LogbookDocument.EVENTS);
            JsonNode lastEvent = Iterables.getLast(events);
            String evDetData = lastEvent.get("evDetData").textValue();
            JsonNode traceabilityEvent = JsonHandler.getFromString(evDetData);
            String fileName = traceabilityEvent.get("FileName").textValue();
            StorageCollectionType documentType = StorageCollectionType.LOGBOOKS;
            final Response response = storageClient.getContainerAsync("default", fileName, documentType);
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                helper.writeResponse(Response
                    .ok()
                    .header("Content-Disposition", "filename=" + fileName)
                    .header("Content-Type", "application/octet-stream"));
            } else {
                helper.writeResponse(Response.status(response.getStatus()));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException was thrown : ", e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (StorageNotFoundException e) {
            LOGGER.error("Storage error was thrown : ", e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, Response.status(Status.NOT_FOUND).build());
        } catch (StorageServerClientException e) {
            LOGGER.error("Storage error was thrown : ", e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        } catch (LogbookClientException e) {
            LOGGER.error("Logbook Client NOT FOUND Exception ", e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, Response.status(Status.NOT_FOUND).build());
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }

}

