/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.configuration.FunctionalAdminAdmin;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.xsrf.filter.XSRFFilter;
import fr.gouv.vitam.common.xsrf.filter.XSRFHelper;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import fr.gouv.vitam.ihmdemo.common.pagination.OffsetBasedPagination;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.TenantLogbookOperationTraceabilityResult;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.storage.driver.model.StorageLogBackupResult;
import fr.gouv.vitam.storage.driver.model.StorageLogTraceabilityResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.gouv.vitam.common.auth.web.filter.CertUtils.REQUEST_PERSONAL_CERTIFICATE_ATTRIBUTE;

/**
 * Web Application Resource class
 */
@Path("/v1/api")
public class WebApplicationResource extends ApplicationStatusResource {

    public static final String DEFAULT_CONTRACT_NAME = "default_contract";
    private static final String RESULTS_FIELD = "$results";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResource.class);
    /**
     * field of VitamResponseError
     */
    private static final String IHM_RECETTE = "IHM_RECETTE";
    private static final String ACCESS_EXTERNAL_MODULE = "AccessExternalModule";
    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";
    private static final String INTERNAL_SERVER_ERROR_MSG = "INTERNAL SERVER ERROR";
    private static final String BAD_REQUEST_EXCEPTION_MSG = "Bad request Exception";
    private static final String ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Access client unavailable";
    private static final String ACCESS_SERVER_EXCEPTION_MSG = "Access Server exception";
    private static final String REQUEST_METHOD_UNDEFINED = "Request method undefined for collection";
    // TODO FIX_TENANT_ID (LFET FOR ONLY stat API)
    private static final Integer TENANT_ID = 0;
    private static final String X_REQUESTED_COLLECTION = "X-Requested-Collection";
    private static final String X_OBJECT_ID = "X-Object-Id";
    private static final String X_REQUEST_ID = "X-Request-Id";

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
    private static final String RULE_ACTIONS = "ruleActions";

    private final UserInterfaceTransactionManager userInterfaceTransactionManager;
    private final PaginationHelper paginationHelper;
    private final DslQueryHelper dslQueryHelper;
    private final StorageService storageService;
    private final ExecutorService threadPoolExecutor = Executors.newCachedThreadPool(VitamThreadFactory.getInstance());
    private final List<String> secureMode;
    private final FunctionalAdminAdmin functionalAdminAdmin;

    /**
     * Constructor
     *
     * @param webApplicationConfigonfig configuration
     */
    public WebApplicationResource(
        WebApplicationConfig webApplicationConfigonfig,
        UserInterfaceTransactionManager userInterfaceTransactionManager,
        PaginationHelper paginationHelper,
        DslQueryHelper dslQueryHelper,
        StorageService storageService
    ) {
        super(new BasicVitamStatusServiceImpl());
        this.secureMode = webApplicationConfigonfig.getSecureMode();
        this.userInterfaceTransactionManager = userInterfaceTransactionManager;
        this.paginationHelper = paginationHelper;
        this.dslQueryHelper = dslQueryHelper;
        this.storageService = storageService;
        this.functionalAdminAdmin = webApplicationConfigonfig.getFunctionalAdminAdmin();
        LOGGER.debug("init Admin Management Resource server");

        WorkspaceClientFactory.changeMode(webApplicationConfigonfig.getWorkspaceUrl());
    }

    /**
     * Returns session id for the authenticated user.
     * <p>
     * The application may track each logged user by a unique session id. This session id is passed to vitam and is
     * persisted "as is" in Vitam logbook operations. In case of audit / legal dispute, the application session id can
     * be used for correlation with application user login logs / db.
     *
     * @return application session id
     */
    private static String getAppSessionId() {
        // TODO : Implement session id -> user mapping persistence (login activity journal / logs...).
        return "MyApplicationId-ChangeIt";
    }

    /**
     * @param xTenantId xtenant
     * @param uid uid
     * @param dataType data
     * @return
     */
    @POST
    @Path("/replaceObject/{dataType}/{strategyId}/{offerId}/{uid}/{size}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadObject(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("uid") String uid,
        @PathParam("dataType") String dataType,
        @PathParam("strategyId") String strategyId,
        @PathParam("offerId") String offerId,
        @PathParam("size") Long size,
        InputStream input
    ) {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));

            StorageCRUDUtils storageCRUDUtils = new StorageCRUDUtils();

            DataCategory dataCategory = DataCategory.valueOf(dataType);

            storageCRUDUtils.storeInOffer(dataCategory, uid, strategyId, offerId, size, input);

            return Response.status(Status.OK).build();
        } catch (BackupServiceException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    /**
     * @param xTenantId xtenant
     * @return list of strategies
     */
    @GET
    @Path("/strategies")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStrategies(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));
            RequestResponse<StorageStrategy> requestResponse = storageClient.getStorageStrategies();
            if (requestResponse.isOk()) {
                return Response.status(Status.OK)
                    .entity(((RequestResponseOK<StorageStrategy>) requestResponse).getResults())
                    .build();
            } else if (requestResponse instanceof VitamError) {
                LOGGER.error(requestResponse.toString());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(requestResponse).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageServerClientException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param xTenantId xtenant
     * @param uid uid
     * @param dataType data
     * @return
     */
    @DELETE
    @Path("/deleteObject/{dataType}/{strategyId}/{offerId}/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteObject(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("uid") String uid,
        @PathParam("dataType") String dataType,
        @PathParam("strategyId") String strategyId,
        @PathParam("offerId") String offerId
    ) {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));

            StorageCRUDUtils storageCRUDUtils = new StorageCRUDUtils();

            DataCategory dataCategory = DataCategory.valueOf(dataType);

            boolean deleted = storageCRUDUtils.deleteFile(dataCategory, uid, strategyId, offerId);
            if (deleted) {
                return Response.status(Status.OK).build();
            }

            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageServerClientException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * launch Rectification audit from recette
     *
     * @param xTenantId xTenantId
     * @param operationId operationId
     * @return
     */
    @POST
    @Path("/launchAudit/{operationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response launchAudit(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @HeaderParam(GlobalDataRest.X_ACCESS_CONTRAT_ID) String xAccessContratId,
        @PathParam("operationId") String operationId
    ) {
        try (AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient()) {
            VitamContext context = new VitamContext(Integer.parseInt(xTenantId));
            context.setAccessContract(xAccessContratId).setApplicationSessionId(getAppSessionId());

            RequestResponse requestResponse = client.rectificationAudit(context, operationId);

            if (requestResponse instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(requestResponse).build();
            }
            if (requestResponse instanceof VitamError) {
                LOGGER.error(requestResponse.toString());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(requestResponse).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (VitamClientException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/ingestcleanup/{operationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response launchIngestCleanup(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @HeaderParam(GlobalDataRest.X_ACCESS_CONTRAT_ID) String xAccessContratId,
        @PathParam("operationId") String operationId
    ) {
        VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));

        // Hack to invoke the admin port of functional admin
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(
            String.format(
                "http://%s:%s/adminmanagement/v1/invalidIngestCleanup/%s",
                this.functionalAdminAdmin.getFunctionalAdminServerHost(),
                this.functionalAdminAdmin.getFunctionalAdminServerPort(),
                operationId
            )
        );
        String basicAuth =
            "Basic " +
            BaseXx.getBase64(
                (this.functionalAdminAdmin.getAdminBasicAuth().getUserName() +
                    ":" +
                    this.functionalAdminAdmin.getAdminBasicAuth().getPassword()).getBytes()
            );

        Invocation.Builder builder = target.request();
        Response response = builder
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .header("Accept", MediaType.APPLICATION_JSON)
            .header("X-Tenant-Id", xTenantId)
            .header("Authorization", basicAuth)
            .post(null);

        if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
            LOGGER.error("Ingest cleanup failed with status " + response.getStatus());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Status.CREATED).build();
    }

    /**
     * Retrieve an Object data as an input stream. Download by access.
     */
    @GET
    @Path("/download/{strategyId}/{offerId}/{dataType}/{uid}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getObjectAsInputStreamAsync(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("uid") String uid,
        @PathParam("dataType") String dataType,
        @PathParam("strategyId") String strategyId,
        @PathParam("offerId") String offerId
    ) {
        VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));

        try {
            return storageService.download(
                VitamThreadUtils.getVitamSession().getTenantId(),
                DataCategory.valueOf(dataType),
                strategyId,
                offerId,
                uid
            );
        } catch (StorageTechnicalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return buildError(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage()).toResponse();
        } catch (StorageDriverUnavailableDataFromAsyncOfferException e) {
            LOGGER.error("No active access request found", e);
            return buildCustomError(CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER, e.getMessage());
        } catch (StorageDriverException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return buildError(VitamCode.STORAGE_OFFER_NOT_FOUND, e.getMessage()).toResponse();
        } catch (StorageNotFoundException e) {
            LOGGER.warn("Not found", e);
            return buildError(VitamCode.STORAGE_NOT_FOUND, e.getMessage()).toResponse();
        }
    }

    private VitamError<JsonNode> buildError(VitamCode vitamCode, String message) {
        return new VitamError<JsonNode>(VitamCodeHelper.getCode(vitamCode))
            .setContext(vitamCode.getService().getName())
            .setHttpCode(vitamCode.getStatus().getStatusCode())
            .setState(vitamCode.getDomain().getName())
            .setMessage(vitamCode.getMessage())
            .setDescription(message);
    }

    private Response buildCustomError(CustomVitamHttpStatusCode customStatusCode, String message) {
        return Response.status(customStatusCode.getStatusCode())
            .entity(
                new RequestResponseError()
                    .setError(
                        new VitamError(customStatusCode.toString())
                            .setContext(IHM_RECETTE)
                            .setHttpCode(customStatusCode.getStatusCode())
                            .setMessage(customStatusCode.getMessage())
                            .setDescription(Strings.isNullOrEmpty(message) ? customStatusCode.getMessage() : message)
                    )
                    .toString()
            )
            .build();
    }

    /**
     * Create an access request for the given offerId, dataType and uid.
     */
    @POST
    @Path("/access-request/{strategyId}/{offerId}/{dataType}/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccessRequest(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("strategyId") String strategyId,
        @PathParam("offerId") String offerId,
        @PathParam("dataType") String dataType,
        @PathParam("uid") String uid
    ) {
        VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));

        try {
            return storageService
                .createAccessRequest(
                    Integer.parseInt(xTenantId),
                    strategyId,
                    offerId,
                    uid,
                    DataCategory.valueOf(dataType)
                )
                .toResponse();
        } catch (StorageTechnicalException | StorageNotFoundException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return buildError(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage()).toResponse();
        }
    }

    /**
     * Check the status of the Access Request @accessRequestId.
     */
    @GET
    @Path("/access-request/{strategyId}/{offerId}/{accessRequestId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkAccessRequestStatus(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("strategyId") String strategyId,
        @PathParam("offerId") String offerId,
        @PathParam("accessRequestId") String accessRequestId
    ) {
        VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));

        try {
            return storageService
                .checkAccessRequestStatus(Integer.parseInt(xTenantId), strategyId, offerId, accessRequestId)
                .toResponse();
        } catch (StorageTechnicalException | StorageNotFoundException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return buildError(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage()).toResponse();
        }
    }

    /**
     * Remove the access request @accessRequestId.
     */
    @DELETE
    @Path("/access-request/{strategyId}/{offerId}/{accessRequestId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeAccessRequest(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("strategyId") String strategyId,
        @PathParam("offerId") String offerId,
        @PathParam("accessRequestId") String accessRequestId
    ) {
        VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));
        try {
            RequestResponse<AccessRequestStatus> readOrderRequest = storageService.removeAccessRequest(
                Integer.parseInt(xTenantId),
                strategyId,
                offerId,
                accessRequestId
            );
            return readOrderRequest.toResponse();
        } catch (StorageTechnicalException | StorageNotFoundException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return buildError(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage()).toResponse();
        }
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
    public Response login(@Context HttpServletRequest httpRequest, JsonNode object) {
        final Subject subject = ThreadContext.getSubject();
        final String username = object.get("token").get("principal").textValue();
        final String password = object.get("token").get("credentials").textValue();

        if (username == null || password == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        final UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        final String tokenCSRF = XSRFHelper.generateCSRFToken();
        XSRFFilter.addToken(httpRequest.getSession().getId(), tokenCSRF);

        try {
            subject.login(token);
            // TODO P1 add access log
            LOGGER.info("Login success: " + username);
        } catch (final Exception uae) {
            LOGGER.debug("Login fail: " + username);
            return Response.status(Status.UNAUTHORIZED).build();
        }

        return Response.status(Status.OK).entity(new LoginModel(tokenCSRF)).build();
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
            VitamContext context = new VitamContext(TENANT_ID);
            context.setAccessContract(DEFAULT_CONTRACT_NAME).setApplicationSessionId(getAppSessionId());

            final RequestResponse<LogbookOperation> logbookOperationResult =
                userInterfaceTransactionManager.selectOperationbyId(operationId, context);
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
     * Return authentication mode
     *
     * @return liste of authentication mode
     */
    @GET
    @Path("/securemode")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSecureMode() {
        return Response.status(Status.OK).entity(this.secureMode).build();
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
        try (
            final LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance()
                .getClient()
        ) {
            VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
            RequestResponseOK<TenantLogbookOperationTraceabilityResult> result = logbookOperationsClient.traceability(
                Collections.singletonList(Integer.parseInt(xTenantId))
            );
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("The reporting json can't be created", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * launch the traceability for unit lifecycles
     *
     * @param xTenantId the tenant id
     * @return the response of the request
     * @throws LogbookClientServerException if logbook internal resources exception occurred
     */
    @POST
    @Path("/lifecycles/units/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceabilityLfcUnit(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId)
        throws LogbookClientServerException {
        try (
            final LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance()
                .getClient()
        ) {
            RequestResponseOK<String> result;
            try {
                VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));
                result = logbookOperationsClient.traceabilityLfcUnit();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error("The reporting json can't be created", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Status.OK).entity(result).build();
        }
    }

    /**
     * launch the traceability for object group lifecycles
     *
     * @param xTenantId the tenant id
     * @return the response of the request
     * @throws LogbookClientServerException if logbook internal resources exception occurred
     */
    @POST
    @Path("/lifecycles/objectgroups/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceabilityLfcObjectGroup(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId)
        throws LogbookClientServerException {
        try (
            final LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance()
                .getClient()
        ) {
            RequestResponseOK result;
            try {
                VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(xTenantId));
                result = logbookOperationsClient.traceabilityLfcObjectGroup();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error("The reporting json can't be created", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Status.OK).entity(result).build();
        }
    }

    /**
     * launch the traceabiity for storage
     *
     * @param xTenantId the tenant id
     * @return the response of the request
     * @throws LogbookClientServerException if logbook internal resources exception occurred
     */
    @POST
    @Path("/storages/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceabilityStorage(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            RequestResponseOK<StorageLogTraceabilityResult> result;
            try {
                VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
                result = storageClient.storageLogTraceability(Collections.singletonList(Integer.parseInt(xTenantId)));
            } catch (final InvalidParseOperationException | StorageServerClientException e) {
                LOGGER.error("The reporting json can't be created", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Status.OK).entity(result).build();
        }
    }

    /**
     * launch the traceabiity for storage
     *
     * @param xTenantId the tenant id
     * @return the response of the request
     * @throws LogbookClientServerException if logbook internal resources exception occurred
     */
    @POST
    @Path("storages/storagelogbackup")
    @Produces(MediaType.APPLICATION_JSON)
    public Response storageLogBackup(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        // /!\ Storage log backup will be done on a single instance.
        //     If you need de backup on all instances, you need to retry multiple times since eventually all storage nodes are reached
        try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            RequestResponseOK<StorageLogBackupResult> result;
            try {
                VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
                result = storageClient.storageLogBackup(VitamConfiguration.getTenants());
            } catch (final InvalidParseOperationException | StorageServerClientException e) {
                LOGGER.error("The reporting json can't be created", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Status.OK).entity(result).build();
        }
    }

    /**
     * Post used because Angular not support Get with body
     *
     * @param request the request
     * @param xhttpOverride the use of http override POST method
     * @param sessionId the id of session
     * @param options the option for creating query to find logbook
     * @return Response
     */
    @POST
    @Path("/logbooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResultByBrowser(
        @Context HttpServletRequest request,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        if (!"GET".equalsIgnoreCase(xhttpOverride)) {
            final Status status = Status.PRECONDITION_FAILED;
            VitamError<JsonNode> vitamError = new VitamError<JsonNode>(status.name())
                .setHttpCode(status.getStatusCode())
                .setContext(IHM_RECETTE)
                .setMessage(status.getReasonPhrase())
                .setDescription(status.getReasonPhrase());
            return Response.status(status).entity(vitamError).build();
        }

        return findLogbookBy(request, sessionId, options);
    }

    /**
     * Update link between 2 AU send in the select request
     *
     * @param request the HTTP request and all its context
     * @param select select query with the following structure: {parentId: 'id', childId: 'id', action: 'ADD/DELETE'}
     */
    @POST
    @Path("/updateLinks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLinksBetweenAU(@Context HttpServletRequest request, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSelectAndUpdateDSLQuery(optionsMap);

            try (
                final AccessExternalClient accessExternalClient = AccessExternalClientFactory.getInstance().getClient()
            ) {
                RequestResponse<JsonNode> response = accessExternalClient.reclassification(
                    getVitamContext(request),
                    query
                );
                if (response instanceof RequestResponseOK) {
                    return Response.status(Status.OK)
                        .entity(response)
                        .header(X_REQUEST_ID, response.getHeaderString(X_REQUEST_ID))
                        .build();
                }
                if (response instanceof VitamError) {
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
     * this method is used to request logbook with the Vitam DSL
     *
     * @param request request http
     * @param sessionId using for pagination
     * @param options JSON object representing the Vitam DSL query
     * @return Response
     */
    @GET
    @Path("/logbooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResult(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        return findLogbookBy(request, sessionId, options);
    }

    private Response findLogbookBy(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        ParametersChecker.checkParameter("cookie is mandatory", sessionId);
        final String xTenantId = request.getHeader(GlobalDataRest.X_TENANT_ID);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        int tenantId = Integer.parseInt(xTenantId);
        String requestId;
        RequestResponse<LogbookOperation> result;
        OffsetBasedPagination pagination;

        try {
            Enumeration<String> headersReqId = request.getHeaders(IhmWebAppHeader.REQUEST_ID.name());
            while (headersReqId.hasMoreElements()) {
                SanityChecker.checkParameter(headersReqId.nextElement());
            }
            // VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            pagination = new OffsetBasedPagination(request);
        } catch (final VitamException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        final List<String> requestIds = Collections.list(request.getHeaders(IhmWebAppHeader.REQUEST_ID.name()));
        if (!requestIds.isEmpty()) {
            requestId = requestIds.get(0);
            // get result from shiro session
            try {
                result = RequestResponseOK.getFromJsonNode(
                    paginationHelper.getResult(sessionId, pagination),
                    LogbookOperation.class
                );

                return Response.status(Status.OK)
                    .entity(result)
                    .header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                    .header(IhmDataRest.X_LIMIT, pagination.getLimit())
                    .build();
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
                final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

                LOGGER.debug("query >>>>>>>>>>>>>>>>> : " + query);
                result = userInterfaceTransactionManager.selectOperation(
                    query,
                    userInterfaceTransactionManager.getVitamContext(request)
                );

                // save result
                LOGGER.debug("resultr <<<<<<<<<<<<<<<<<<<<<<<: " + result);
                paginationHelper.setResult(sessionId, result.toJsonNode());
                // pagination
                result = RequestResponseOK.getFromJsonNode(
                    paginationHelper.getResult(result.toJsonNode(), pagination),
                    LogbookOperation.class
                );
            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
            } catch (final LogbookClientException e) {
                LOGGER.error("Logbook Client NOT FOUND Exception ", e);
                return Response.status(Status.NOT_FOUND).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
            } catch (final Exception e) {
                LOGGER.error("Internal server error", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .build();
            }
            return Response.status(Status.OK)
                .entity(result)
                .header(GlobalDataRest.X_REQUEST_ID, requestId)
                .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                .header(IhmDataRest.X_LIMIT, pagination.getLimit())
                .build();
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
    public Response getLogbookResultById(
        @PathParam("idOperation") String operationId,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId
    ) {
        try {
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            int tenantId = Integer.parseInt(xTenantId);
            VitamContext context = new VitamContext(tenantId);
            context.setAccessContract(DEFAULT_CONTRACT_NAME).setApplicationSessionId(getAppSessionId());
            final RequestResponse<LogbookOperation> result = userInterfaceTransactionManager.selectOperationbyId(
                operationId,
                context
            );
            return Response.status(Status.OK).entity(result).build();
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Vitam Client NOT FOUND Exception ", e);
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
    public void downloadObjectAsStream(
        @PathParam("idOperation") String operationId,
        @Suspended final AsyncResponse asyncResponse,
        @QueryParam(GlobalDataRest.X_TENANT_ID) String xTenantId
    ) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
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
    public void downloadObjectAsStreamForBrowser(
        @PathParam("idOperation") String operationId,
        @Suspended final AsyncResponse asyncResponse,
        @QueryParam(GlobalDataRest.X_TENANT_ID) Integer tenantId
    ) {
        threadPoolExecutor.execute(() -> downloadObjectAsync(asyncResponse, operationId, tenantId));
    }

    /**
     * download object for LOGBOOKS type
     *
     * @param asyncResponse
     * @param operationId
     */
    private void downloadObjectAsync(final AsyncResponse asyncResponse, String operationId, int tenantId) {
        Response response = null;
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            VitamContext context = new VitamContext(tenantId);
            context.setAccessContract(DEFAULT_CONTRACT_NAME).setApplicationSessionId(getAppSessionId());
            final RequestResponse<LogbookOperation> result = userInterfaceTransactionManager.selectOperationbyId(
                operationId,
                context
            );

            RequestResponseOK<LogbookOperation> responseOK = (RequestResponseOK<LogbookOperation>) result;
            LogbookOperation operation = responseOK.getFirstResult();
            LogbookEventOperation lastEvent = Iterables.getLast(operation.getEvents());

            String evDetData = lastEvent.getEvDetData();
            JsonNode traceabilityEvent = JsonHandler.getFromString(evDetData);
            String fileName = traceabilityEvent.get("FileName").textValue();
            DataCategory documentType = DataCategory.LOGBOOK;
            response = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                fileName,
                documentType,
                AccessLogUtils.getNoLogAccessLog()
            );
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                helper.writeResponse(
                    Response.ok()
                        .header("Content-Disposition", "filename=" + fileName)
                        .header("Content-Type", "application/octet-stream")
                );
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
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build()
            );
        } catch (StorageUnavailableDataFromAsyncOfferClientException e) {
            LOGGER.error("No active access request found", e);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                buildCustomError(CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER, e.getMessage())
            );
        } catch (VitamClientException e) {
            LOGGER.error("Vitam Client NOT FOUND Exception ", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.NOT_FOUND).build());
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build()
            );
        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Query to get Access contracts
     *
     * @param request HTTP request
     * @param select the query to find access contracts
     * @return Response
     */
    @POST
    @Path("/accesscontracts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessContract(@Context HttpServletRequest request, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
                RequestResponse<AccessContractModel> response = adminClient.findAccessContracts(
                    getVitamContext(request),
                    query
                );
                if (response instanceof RequestResponseOK) {
                    return Response.status(Status.OK).entity(response).build();
                }
                if (response instanceof VitamError) {
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

    @POST
    @Path("/dslQueryTest")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAndExecuteTestRequest(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        JsonNode criteria
    ) {
        String requestId;

        RequestResponse<?> result;
        OffsetBasedPagination pagination;
        String requestMethod = request.getHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE);
        String requestedCollection = request.getHeader(X_REQUESTED_COLLECTION);
        String objectID = request.getHeader(X_OBJECT_ID);
        String xAction = request.getHeader(GlobalDataRest.X_ACTION);

        try {
            pagination = new OffsetBasedPagination(request);
            Enumeration<String> headersReqId = request.getHeaders(IhmWebAppHeader.REQUEST_ID.name());
            while (headersReqId.hasMoreElements()) {
                SanityChecker.checkParameter(headersReqId.nextElement());
            }
        } catch (final VitamException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        final List<String> requestIds = Collections.list(request.getHeaders(IhmWebAppHeader.REQUEST_ID.name()));

        if (!requestIds.isEmpty()) {
            requestId = requestIds.get(0);
            // get result from shiro session
            try {
                result = RequestResponseOK.getFromJsonNode(paginationHelper.getResult(sessionId, pagination));

                return Response.status(Status.OK)
                    .entity(result)
                    .header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                    .header(IhmDataRest.X_LIMIT, pagination.getLimit())
                    .build();
            } catch (final VitamException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
            }
        } else {
            try {
                AdminCollections requestedAdminCollection = existsInAdminCollections(requestedCollection);
                if (requestedCollection != null && requestedAdminCollection == null) {
                    if (
                        !(requestedCollection.equalsIgnoreCase(WORKFLOW_OPERATIONS) ||
                            requestedCollection.equalsIgnoreCase(WORKFLOWS))
                    ) {
                        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
                            if (requestedCollection.equalsIgnoreCase(UNIT_COLLECTION)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        if (StringUtils.isBlank(objectID)) {
                                            result = client.selectUnits(getVitamContext(request), criteria);
                                        } else {
                                            result = client.selectUnitbyId(
                                                getVitamContext(request),
                                                criteria,
                                                objectID
                                            );
                                        }
                                        break;
                                    case HTTP_PUT:
                                        if (StringUtils.isNotBlank(objectID)) {
                                            result = client.updateUnitbyId(
                                                getVitamContext(request),
                                                criteria,
                                                objectID
                                            );
                                        } else if (criteria.get(RULE_ACTIONS) != null) {
                                            result = client.massUpdateUnitsRules(getVitamContext(request), criteria);
                                        } else {
                                            result = client.massUpdateUnits(getVitamContext(request), criteria);
                                        }
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                        );
                                }
                            } else if (requestedCollection.equalsIgnoreCase(LOGBOOK_COLLECTION)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        if (StringUtils.isBlank(objectID)) {
                                            result = client.selectOperations(getVitamContext(request), criteria);
                                        } else {
                                            result = client.selectOperationbyId(
                                                getVitamContext(request),
                                                objectID,
                                                criteria
                                            );
                                        }
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                        );
                                }
                            } else if (requestedCollection.equalsIgnoreCase(OBJECT_GROUP_COLLECTION)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        if (StringUtils.isBlank(objectID)) {
                                            result = client.selectObjects(getVitamContext(request), criteria);
                                        } else {
                                            result = client.selectObjectMetadatasByUnitId(
                                                getVitamContext(request),
                                                criteria,
                                                objectID
                                            );
                                            if (result != null) {
                                                return Response.status(Status.OK).entity(result.toJsonNode()).build();
                                            }
                                        }
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                        );
                                }
                            } else if (requestedCollection.equalsIgnoreCase(UNIT_LIFECYCLES)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        result = client.selectUnitLifeCycleById(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                        );
                                }
                            } else if (requestedCollection.equalsIgnoreCase(OBJECT_GROUP_LIFECYCLES)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        result = client.selectObjectGroupLifeCycleById(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                        );
                                }
                            } else {
                                throw new UnsupportedOperationException("Collection unrecognized");
                            }
                        }
                    } else {
                        try (
                            AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance()
                                .getClient()
                        ) {
                            if (requestedCollection.equalsIgnoreCase(WORKFLOW_OPERATIONS)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        if (StringUtils.isBlank(objectID)) {
                                            if (criteria != null) {
                                                LOGGER.error("criteria not null");
                                                result = adminExternalClient.listOperationsDetails(
                                                    getVitamContext(request),
                                                    JsonHandler.getFromJsonNode(criteria, ProcessQuery.class)
                                                );
                                            } else {
                                                LOGGER.error("criteria null");
                                                result = adminExternalClient.listOperationsDetails(
                                                    getVitamContext(request),
                                                    null
                                                );
                                            }

                                            return Response.status(Status.OK).entity(result).build();
                                        } else {
                                            result = adminExternalClient.getOperationProcessExecutionDetails(
                                                getVitamContext(request),
                                                objectID
                                            );
                                            return result.toResponse();
                                        }
                                    case HTTP_PUT:
                                        if (!StringUtils.isBlank(objectID)) {
                                            adminExternalClient.updateOperationActionProcess(
                                                getVitamContext(request),
                                                xAction,
                                                objectID
                                            );
                                            result = adminExternalClient.getOperationProcessExecutionDetails(
                                                getVitamContext(request),
                                                objectID
                                            );
                                            return result.toResponse();
                                        } else {
                                            throw new InvalidParseOperationException("Operation ID should be filled");
                                        }
                                    case HTTP_DELETE:
                                        if (!StringUtils.isBlank(objectID)) {
                                            result = adminExternalClient.cancelOperationProcessExecution(
                                                getVitamContext(request),
                                                objectID
                                            );
                                            return result.toResponse();
                                        } else {
                                            throw new InvalidParseOperationException("Operation ID should be filled");
                                        }
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                        );
                                }
                            } else if (requestedCollection.equalsIgnoreCase(WORKFLOWS)) {
                                switch (requestMethod) {
                                    case HTTP_GET:
                                        result = adminExternalClient.getWorkflowDefinitions(getVitamContext(request));
                                        return Response.status(Status.OK).entity(result).build();
                                    default:
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                        );
                                }
                            }
                            throw new UnsupportedOperationException(
                                "No implementation found for collection " + requestedCollection
                            );
                        }
                    }
                } else {
                    requestedAdminCollection = AdminCollections.valueOf(requestedCollection);
                    try (
                        AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()
                    ) {
                        switch (requestMethod) {
                            case HTTP_GET:
                                if (StringUtils.isBlank(objectID)) {
                                    switch (requestedAdminCollection) {
                                        case FORMATS:
                                            result = adminExternalClient.findFormats(
                                                getVitamContext(request),
                                                criteria
                                            );
                                            break;
                                        case RULES:
                                            result = adminExternalClient.findRules(getVitamContext(request), criteria);
                                            break;
                                        case ACCESS_CONTRACTS:
                                            result = adminExternalClient.findAccessContracts(
                                                getVitamContext(request),
                                                criteria
                                            );
                                            break;
                                        case INGEST_CONTRACTS:
                                            result = adminExternalClient.findIngestContracts(
                                                getVitamContext(request),
                                                criteria
                                            );
                                            break;
                                        case MANAGEMENT_CONTRACTS:
                                            result = adminExternalClient.findManagementContracts(
                                                getVitamContext(request),
                                                criteria
                                            );
                                            break;
                                        case CONTEXTS:
                                            result = adminExternalClient.findContexts(
                                                getVitamContext(request),
                                                criteria
                                            );
                                            break;
                                        case PROFILE:
                                            result = adminExternalClient.findProfiles(
                                                getVitamContext(request),
                                                criteria
                                            );
                                            break;
                                        case ACCESSION_REGISTERS:
                                            result = adminExternalClient.findAccessionRegister(
                                                getVitamContext(request),
                                                criteria
                                            );
                                            break;
                                        case AGENCIES:
                                            result = adminExternalClient.findAgencies(
                                                getVitamContext(request),
                                                criteria
                                            );
                                            break;
                                        default:
                                            throw new UnsupportedOperationException(
                                                "No implementation found for collection " + requestedCollection
                                            );
                                    }
                                } else {
                                    if (AdminCollections.ACCESSION_REGISTERS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.getAccessionRegisterDetail(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                    } else {
                                        switch (requestedAdminCollection) {
                                            case FORMATS:
                                                result = adminExternalClient.findFormatById(
                                                    getVitamContext(request),
                                                    objectID
                                                );
                                                break;
                                            case RULES:
                                                result = adminExternalClient.findRuleById(
                                                    getVitamContext(request),
                                                    objectID
                                                );
                                                break;
                                            case ACCESS_CONTRACTS:
                                                result = adminExternalClient.findAccessContractById(
                                                    getVitamContext(request),
                                                    objectID
                                                );
                                                break;
                                            case INGEST_CONTRACTS:
                                                result = adminExternalClient.findIngestContractById(
                                                    getVitamContext(request),
                                                    objectID
                                                );
                                                break;
                                            case MANAGEMENT_CONTRACTS:
                                                result = adminExternalClient.findManagementContractById(
                                                    getVitamContext(request),
                                                    objectID
                                                );
                                                break;
                                            case CONTEXTS:
                                                result = adminExternalClient.findContextById(
                                                    getVitamContext(request),
                                                    objectID
                                                );
                                                break;
                                            case PROFILE:
                                                result = adminExternalClient.findProfileById(
                                                    getVitamContext(request),
                                                    objectID
                                                );
                                                break;
                                            case AGENCIES:
                                                result = adminExternalClient.findAgencyByID(
                                                    getVitamContext(request),
                                                    objectID
                                                );
                                                break;
                                            default:
                                                throw new UnsupportedOperationException(
                                                    "No implementation found for collection " + requestedCollection
                                                );
                                        }
                                    }
                                }
                                break;
                            case HTTP_PUT:
                                if (!StringUtils.isBlank(objectID)) {
                                    if (AdminCollections.CONTEXTS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateContext(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                    } else if (AdminCollections.ACCESS_CONTRACTS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateAccessContract(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                    } else if (AdminCollections.INGEST_CONTRACTS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateIngestContract(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                    } else if (AdminCollections.MANAGEMENT_CONTRACTS.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateManagementContract(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                    } else if (AdminCollections.PROFILE.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateProfile(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                    } else if (AdminCollections.SECURITY_PROFILES.equals(requestedAdminCollection)) {
                                        result = adminExternalClient.updateSecurityProfile(
                                            getVitamContext(request),
                                            objectID,
                                            criteria
                                        );
                                    } else {
                                        throw new UnsupportedOperationException(
                                            REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                        );
                                    }
                                } else {
                                    throw new InvalidParseOperationException("Unit ID should be filled.");
                                }
                                break;
                            default:
                                throw new UnsupportedOperationException(
                                    REQUEST_METHOD_UNDEFINED + " " + requestedCollection
                                );
                        }
                    }
                }

                return Response.status(Status.OK).entity(result).build();
            } catch (final InvalidParseOperationException | IllegalArgumentException e) {
                LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
                VitamError<JsonNode> vitamError = new VitamError<JsonNode>(VitamCode.GLOBAL_EMPTY_QUERY.getItem())
                    .setHttpCode(Status.BAD_REQUEST.getStatusCode())
                    .setContext(IHM_RECETTE)
                    .setState(StatusCode.KO.name())
                    .setMessage(Status.BAD_REQUEST.getReasonPhrase())
                    .setDescription(e.getMessage());
                return vitamError.toResponse();
            } catch (final AccessExternalClientServerException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                VitamError<JsonNode> vitamError = new VitamError<JsonNode>(
                    VitamCode.ACCESS_EXTERNAL_SERVER_ERROR.getItem()
                )
                    .setHttpCode(VitamCode.ACCESS_EXTERNAL_SERVER_ERROR.getStatus().getStatusCode())
                    .setContext(ACCESS_EXTERNAL_MODULE)
                    .setState(StatusCode.KO.name())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SERVER_ERROR.getMessage())
                    .setDescription(e.getMessage());
                return vitamError.toResponse();
            } catch (final AccessExternalClientNotFoundException e) {
                LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
                VitamError<JsonNode> vitamError = new VitamError<JsonNode>(
                    VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR.getItem()
                )
                    .setHttpCode(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR.getStatus().getStatusCode())
                    .setContext(ACCESS_EXTERNAL_MODULE)
                    .setState(StatusCode.KO.name())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR.getMessage())
                    .setDescription(e.getMessage());
                return vitamError.toResponse();
            } catch (final AccessUnauthorizedException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                VitamError<JsonNode> vitamError = new VitamError<JsonNode>(
                    VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR.getItem()
                )
                    .setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                    .setContext(ACCESS_EXTERNAL_MODULE)
                    .setState(Status.UNAUTHORIZED.name())
                    .setMessage(Status.UNAUTHORIZED.getReasonPhrase())
                    .setDescription(e.getMessage());
                return vitamError.toResponse();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                VitamError<JsonNode> vitamError = new VitamError<JsonNode>(
                    VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()
                )
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .setContext(IHM_RECETTE)
                    .setState(StatusCode.KO.name())
                    .setMessage(Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
                    .setDescription(e.getMessage());
                return vitamError.toResponse();
            }
        }
    }

    private Integer getTenantId(HttpServletRequest request) {
        // TODO Error check ? Throw error or put tenant Id 0
        int tenantId = 0;
        String tenantIdHeader = request.getHeader(GlobalDataRest.X_TENANT_ID);
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

    private String getContractId(HttpServletRequest request) {
        // TODO Error check ? Throw error or put tenant Id 0
        return request.getHeader(GlobalDataRest.X_ACCESS_CONTRAT_ID);
    }

    private AdminCollections existsInAdminCollections(String valueToCheck) {
        try {
            return AdminCollections.valueOf(valueToCheck);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private VitamContext getVitamContext(HttpServletRequest request) {
        return new VitamContext(getTenantId(request))
            .setAccessContract(getContractId(request))
            .setApplicationSessionId(getAppSessionId())
            .setPersonalCertificate(getPersonalCertificate(request));
    }

    private String getPersonalCertificate(HttpServletRequest request) {
        return (String) request.getAttribute(REQUEST_PERSONAL_CERTIFICATE_ATTRIBUTE);
    }
}
