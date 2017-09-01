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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.model.StatusCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import fr.gouv.vitam.ihmdemo.common.pagination.OffsetBasedPagination;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;

/**
 * Web Application Resource class
 */
@Path("/v1/api")
public class WebApplicationResource extends ApplicationStatusResource {

    private static final String RESULTS_FIELD = "$results";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResource.class);
    /**
     * field of VitamResponseError
     */
    public static final String IHM_RECETTE = "IHM_RECETTE";
    private static final String ACCESS_EXTERNAL_MODULE = "AccessExternalModule";
    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";
    private static final String INTERNAL_SERVER_ERROR_MSG = "INTERNAL SERVER ERROR";
    private static final String BAD_REQUEST_EXCEPTION_MSG = "Bad request Exception";
    private static final String ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Access client unavailable";
    private static final String ACCESS_SERVER_EXCEPTION_MSG = "Access Server exception";
    private static final String REQUEST_METHOD_UNDEFINED = "Request method undefined for collection";
    private static final String WRONG_QUERY = "Query should not be empty or lacks parameters";


    // TODO FIX_TENANT_ID (LFET FOR ONLY stat API)
    private static final Integer TENANT_ID = 0;
    public static final String DEFAULT_CONTRACT_NAME = "default_contract";

    private static final String X_REQUESTED_COLLECTION = "X-Requested-Collection";
    private static final String X_OBJECT_ID = "X-Object-Id";

    private static final String UNIT_COLLECTION = "UNIT";
    private static final String LOGBOOK_COLLECTION = "LOGBOOK";
    private static final String OBJECT_GROUP_COLLECTION = "OBJECTGROUP";
    private static final String UNIT_LIFECYCLES = "UNITLIFECYCLES";
    private static final String OBJECT_GROUP_LIFECYCLES = "OBJECTGROUPLIFECYCLES";
    private static final String WORKFLOW_OPERATIONS = "OPERATIONS";
    private static final String WORKFLOWS = "WORKFLOWS";

    private static final String HTTP_GET = "GET";
    private static final String HTTP_PUT = "PUT";
    private static final String HTTP_DELETE = "DELETE";

    private ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();

    /**
     * Constructor
     * 
     * @param tenants list of working tenant
     *
     */
    public WebApplicationResource(List<Integer> tenants) {
        super(new BasicVitamStatusServiceImpl(), tenants);

        LOGGER.debug("init Admin Management Resource server");
    }

    /**
     * Retrieve all the messages for logbook
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
                .selectOperationbyId(operationId, TENANT_ID, DEFAULT_CONTRACT_NAME);
            if (logbookOperationResult != null && logbookOperationResult.toJsonNode().has(RESULTS_FIELD)) {
                final JsonNode logbookOperation = logbookOperationResult.toJsonNode().get(RESULTS_FIELD).get(0);
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
     * @param xTenantId the tenant id
     * @return the response of the request
     * @throws LogbookClientServerException if logbook internal resources exception occurred
     */
    @POST
    @Path("/operations/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceability(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId)
        throws LogbookClientServerException {

        try (final LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            RequestResponseOK result;
            try {
                // TODO add tenantId as param
                VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));
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
     * launch the traceabiity for lifecycles
     * 
     * @param xTenantId the tenant id
     * @return the response of the request
     * @throws LogbookClientServerException if logbook internal resources exception occurred
     */
    @POST
    @Path("/lifecycles/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceabilityLFC(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId)
        throws LogbookClientServerException {

        try (final LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            RequestResponseOK result;
            try {
                // TODO add tenantId as param
                VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));
                result = logbookOperationsClient.traceabilityLFC();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error("The reporting json can't be created", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .build();
            }
            return Response.status(Status.OK).entity(result).build();
        }
    }


    /**
     * Post used because Angular not support Get with body
     *
     * @param headers the HttpHeaders for the request
     * @param xhttpOverride the use of http override POST method
     * @param sessionId the id of session
     * @param options the option for creating query to find logbook
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
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        Integer tenantId = null;
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String requestId = null;
        RequestResponse result = null;
        OffsetBasedPagination pagination = null;

        try {
            tenantId = Integer.parseInt(xTenantId);
            // VitamThreadUtils.getVitamSession().setTenantId(tenantId);
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
            requestId = GUIDFactory.newRequestIdGUID(tenantId).toString();

            try {
                ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
                final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);

                LOGGER.debug("query >>>>>>>>>>>>>>>>> : " + query);
                result = UserInterfaceTransactionManager.selectOperation(query, tenantId, DEFAULT_CONTRACT_NAME);

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
     * @param operationId id of operation
     * @param xTenantId the tenant id
     * @return Response
     */
    @GET
    @Path("/logbooks/{idOperation}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResultById(@PathParam("idOperation") String operationId,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        try {
            Integer tenantId = null;
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            tenantId = Integer.parseInt(xTenantId);
            final RequestResponse<JsonNode> result =
                UserInterfaceTransactionManager.selectOperationbyId(operationId, tenantId, DEFAULT_CONTRACT_NAME);
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
     * @param operationId the operation id
     * @param asyncResponse the asynchronized response
     * @param xTenantId the tenant id
     */

    @GET
    @Path("/logbooks/{idOperation}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void downloadObjectAsStream(@PathParam("idOperation") String operationId,
        @Suspended final AsyncResponse asyncResponse, @QueryParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.BAD_REQUEST).build());
        }
        threadPoolExecutor.execute(() -> downloadObjectAsync(asyncResponse, operationId, Integer.parseInt(xTenantId)));
    }

    /**
     * This method exist only to download a file with a browser
     *
     * @param operationId the operation id
     * @param asyncResponse the asynchronized response
     * @param tenantId the working tenant
     */
    @GET
    @Path("/logbooks/{idOperation}/content")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void downloadObjectAsStreamForBrowser(@PathParam("idOperation") String operationId,
        @Suspended final AsyncResponse asyncResponse, @QueryParam(GlobalDataRest.X_TENANT_ID) Integer tenantId) {
        threadPoolExecutor.execute(() -> downloadObjectAsync(asyncResponse, operationId, tenantId));
    }

    /**
     * download object for LOGBOOKS type
     *
     * @param asyncResponse
     * @param operationId
     */
    private void downloadObjectAsync(final AsyncResponse asyncResponse, String operationId, int tenantId) {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            final RequestResponse<JsonNode> result =
                UserInterfaceTransactionManager.selectOperationbyId(operationId, tenantId, DEFAULT_CONTRACT_NAME);

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
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (StorageNotFoundException e) {
            LOGGER.error("Storage error was thrown : ", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.NOT_FOUND).build());
        } catch (StorageServerClientException e) {
            LOGGER.error("Storage error was thrown : ", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        } catch (LogbookClientException e) {
            LOGGER.error("Logbook Client NOT FOUND Exception ", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.NOT_FOUND).build());
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Query to get Access contracts
     *
     * @param headers HTTP Headers
     * @param select the query to find access contracts
     * @return Response
     */
    @POST
    @Path("/accesscontracts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessContract(@Context HttpHeaders headers, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
                RequestResponse response =
                    adminClient.findDocuments(AdminCollections.ACCESS_CONTRACTS, query, getTenantId(headers));
                if (response != null && response instanceof RequestResponseOK) {
                    return Response.status(Status.OK).entity(response).build();
                }
                if (response != null && response instanceof VitamError) {
                    LOGGER.error(response.toString());
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity(response).build();
                }
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param headers needed for the request: X-TENANT-ID , X-Access-Contract-Id, X-Request-Method
     * @param sessionId json session id from shiro
     * @param criteria criteria search for units
     * @return Reponse
     */
    @POST
    @Path("/dslQueryTest")
    @Produces(MediaType.APPLICATION_JSON)
    // @RequiresPermissions("archivesearch:units:read")
    public Response getAndExecuteTestRequest(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
        JsonNode criteria) {
        String requestId;
        RequestResponse result;
        OffsetBasedPagination pagination = null;
        Integer tenantId = getTenantId(headers);
        String contractId = getContractId(headers);
        String requestMethod = headers.getHeaderString(GlobalDataRest.X_HTTP_METHOD_OVERRIDE);
        String requestedCollection = headers.getHeaderString(X_REQUESTED_COLLECTION);
        String objectID = headers.getHeaderString(X_OBJECT_ID);
        String xAction = headers.getHeaderString(GlobalDataRest.X_ACTION);

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
            try {
                AdminCollections requestedAdminCollection = existsInAdminCollections(requestedCollection);
                if (requestedCollection != null && requestedAdminCollection == null) {
                    if (!(requestedCollection.equalsIgnoreCase(WORKFLOW_OPERATIONS) ||
                        requestedCollection.equalsIgnoreCase(WORKFLOWS))) {
                        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
                            if (requestedCollection.equalsIgnoreCase(UNIT_COLLECTION)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        if (StringUtils.isBlank(objectID)) {
                                            result = client.selectUnits(criteria, tenantId,
                                                contractId);
                                        } else {
                                            result = client.selectUnitbyId(criteria, objectID, tenantId,
                                                contractId);
                                        }
                                        break;
                                    case HTTP_PUT:
                                        if (StringUtils.isBlank(objectID)) {
                                            throw new InvalidParseOperationException(
                                                "Unit ID should be filled.");
                                        } else {
                                            result = client.updateUnitbyId(criteria, objectID, tenantId, contractId);
                                        }
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                                }
                            } else if (requestedCollection.equalsIgnoreCase(LOGBOOK_COLLECTION)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        if (StringUtils.isBlank(objectID)) {
                                            result = client.selectOperation(criteria, tenantId,
                                                contractId);
                                        } else {
                                            result = client.selectOperationbyId(objectID, tenantId,
                                                contractId);
                                        }
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                                }
                            } else if (requestedCollection.equalsIgnoreCase(OBJECT_GROUP_COLLECTION)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        if (StringUtils.isBlank(objectID)) {
                                            throw new InvalidParseOperationException(
                                                "Object ID should not be empty for collection " + requestedCollection);
                                        } else {
                                            result = client.selectObjectById(criteria, objectID, tenantId,
                                                contractId);
                                            if (result != null) {
                                                return Response.status(Status.OK)
                                                    .entity(JsonTransformer.transformResultObjects(result.toJsonNode()))
                                                    .build();
                                            }
                                        }
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                                }
                            } else if (requestedCollection.equalsIgnoreCase(UNIT_LIFECYCLES)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        result = client.selectUnitLifeCycleById(objectID, tenantId, contractId);
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                                }
                            } else if (requestedCollection.equalsIgnoreCase(OBJECT_GROUP_LIFECYCLES)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        result = client.selectObjectGroupLifeCycleById(objectID, tenantId, contractId);
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                                }
                            } else {
                                throw new UnsupportedOperationException("Collection unrecognized");
                            }
                        }
                    } else {
                        try (IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance()
                            .getClient()) {
                            if (requestedCollection.equalsIgnoreCase(WORKFLOW_OPERATIONS)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        if (StringUtils.isBlank(objectID)) {
                                            if (criteria != null) {
                                                LOGGER.error("criteria not null");
                                                result = ingestExternalClient.listOperationsDetails(tenantId,
                                                    JsonHandler.getFromJsonNode(criteria, ProcessQuery.class));
                                            } else {
                                                LOGGER.error("criteria null");
                                                result = ingestExternalClient.listOperationsDetails(tenantId, null);
                                            }

                                            return Response.status(Status.OK).entity(result).build();
                                        } else {
                                            result =
                                                ingestExternalClient
                                                    .getOperationProcessExecutionDetails(objectID, tenantId);
                                            return result.toResponse();
                                        }
                                    case HTTP_PUT:
                                        if (!StringUtils.isBlank(objectID)) {
                                            ingestExternalClient.updateOperationActionProcess(xAction, objectID, tenantId);
                                            result = ingestExternalClient.getOperationProcessExecutionDetails(objectID, tenantId);
                                            return result.toResponse();
                                        } else {
                                            throw new InvalidParseOperationException(
                                                "Operation ID should be filled");
                                        }
                                    case HTTP_DELETE:
                                        if (!StringUtils.isBlank(objectID)) {
                                            result = ingestExternalClient.cancelOperationProcessExecution(objectID, tenantId);
                                            return result.toResponse();
                                        } else {
                                            throw new InvalidParseOperationException(
                                                "Operation ID should be filled");
                                        }
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                                }
                            } else if (requestedCollection.equalsIgnoreCase(WORKFLOWS)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        result = ingestExternalClient.getWorkflowDefinitions(tenantId);
                                        return Response.status(Status.OK).entity(result).build();
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                                }
                            }
                            throw new UnsupportedOperationException(
                                "No implementation found for collection " + requestedCollection);
                        }
                    }
                } else {
                    requestedAdminCollection = AdminCollections.valueOf(requestedCollection);
                    try (AdminExternalClient adminExternalClient =
                        AdminExternalClientFactory.getInstance().getClient()) {
                        switch (requestMethod) {
                            case HTTP_GET:
                                contractId = AdminCollections.ACCESSION_REGISTERS.equals(requestedAdminCollection)
                                    ? contractId
                                    : null;
                                if (StringUtils.isBlank(objectID)) {
                                    result = adminExternalClient.findDocuments(requestedAdminCollection, criteria,
                                        tenantId, contractId);
                                } else {
                                    if (AdminCollections.ACCESSION_REGISTERS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.getAccessionRegisterDetail(objectID, criteria,
                                            tenantId, contractId);
                                    } else {
                                        result =
                                            adminExternalClient.findDocumentById(requestedAdminCollection, objectID,
                                                tenantId);
                                    }
                                }
                                break;
                            case HTTP_PUT:
                                if (!StringUtils.isBlank(objectID)) {
                                    if (AdminCollections.CONTEXTS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateContext(objectID, criteria, tenantId);
                                    } else if (AdminCollections.ACCESS_CONTRACTS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateAccessContract(objectID, criteria, tenantId);
                                    } else if (AdminCollections.ENTRY_CONTRACTS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateIngestContract(objectID, criteria, tenantId);
                                    } else {
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                                    }
                                } else {
                                    throw new InvalidParseOperationException(
                                        "Unit ID should be filled.");
                                }
                                break;
                            default:
                                throw new UnsupportedOperationException(
                                    REQUEST_METHOD_UNDEFINED + " " + requestedCollection);
                        }
                    }
                }

                return Response.status(Status.OK).entity(result).build();
            } catch (final InvalidParseOperationException | IllegalArgumentException e) {
                LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
                VitamError vitamError = new VitamError(VitamCode.GLOBAL_EMPTY_QUERY.getItem())
                    .setHttpCode(Status.BAD_REQUEST.getStatusCode())
                    .setContext(IHM_RECETTE).setState(StatusCode.KO.name())
                    .setMessage(Status.BAD_REQUEST.getReasonPhrase()).setDescription(e.getMessage());
                return vitamError.toResponse();
            } catch (final AccessExternalClientServerException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_SERVER_ERROR.getItem())
                    .setHttpCode(VitamCode.ACCESS_EXTERNAL_SERVER_ERROR.getStatus().getStatusCode())
                    .setContext(ACCESS_EXTERNAL_MODULE).setState(StatusCode.KO.name())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SERVER_ERROR.getMessage()).setDescription(e.getMessage());
                return vitamError.toResponse();
            } catch (final AccessExternalClientNotFoundException e) {
                LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
                VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR.getItem())
                    .setHttpCode(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR.getStatus().getStatusCode())
                    .setContext(ACCESS_EXTERNAL_MODULE).setState(StatusCode.KO.name())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR.getMessage()).setDescription(e.getMessage());
                return vitamError.toResponse();
            } catch (final AccessUnauthorizedException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR.getItem())
                    .setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                    .setContext(ACCESS_EXTERNAL_MODULE).setState(Status.UNAUTHORIZED.name())
                    .setMessage(Status.UNAUTHORIZED.getReasonPhrase()).setDescription(e.getMessage());
                return vitamError.toResponse();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                VitamError vitamError = new VitamError(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .setContext(IHM_RECETTE).setState(StatusCode.KO.name())
                    .setMessage(Status.INTERNAL_SERVER_ERROR.getReasonPhrase()).setDescription(e.getMessage());
                return vitamError.toResponse();
            }
        }
    }


    private Integer getTenantId(HttpHeaders headers) {
        // TODO Error check ? Throw error or put tenant Id 0
        Integer tenantId = 0;
        String tenantIdHeader = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        if (tenantIdHeader != null) {
            try {
                tenantId = Integer.parseInt(tenantIdHeader);
            } catch (NumberFormatException e) {
                // TODO Throw error or log something ?
                // Do Nothing : Put 0 as tenant Id
            }
        }
        return tenantId;
    }

    private String getContractId(HttpHeaders headers) {
        // TODO Error check ? Throw error or put tenant Id 0
        String contractId = headers.getHeaderString(GlobalDataRest.X_ACCESS_CONTRAT_ID);
        return contractId;
    }

    private AdminCollections existsInAdminCollections(String valueToCheck) {
        try {
            AdminCollections requestedAdminCollection = AdminCollections.valueOf(valueToCheck);
            return requestedAdminCollection;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

