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
package fr.gouv.vitam.ihmdemo.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalNotFoundException;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.export.dip.DipRequest;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.xsrf.filter.XSRFFilter;
import fr.gouv.vitam.common.xsrf.filter.XSRFHelper;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import fr.gouv.vitam.ihmdemo.common.pagination.OffsetBasedPagination;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.common.utils.PermissionReader;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.server.application.AsyncInputStreamHelper.asyncResponseResume;

/**
 * Web Application Resource class
 */
@Path("/v1/api")
@javax.ws.rs.ApplicationPath("webresources")
public class WebApplicationResource extends ApplicationStatusResource {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String ATTACHMENT_FILENAME_ERROR_REPORT_JSON = "attachment; filename=rapport.json";
    /**
     * X_SIZE_TOTAL
     */
    public static final String X_SIZE_TOTAL = "X-Size-Total";
    /**
     * X_CHUNK_OFFSET
     */
    public static final String X_CHUNK_OFFSET = "X-Chunk-Offset";
    private static final String CSV = ".csv";
    private static final String JSON = ".json";
    private static final String JSONL = ".jsonl";
    private static final String DISTRIBUTION = "distribution";
    private static final String BATCH_REPORT = "batchreport";
    private static final String AGENCIES = "agencies";
    private static final String RULES = "rules";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResource.class);

    private static final String CODE_VITAM = "code_vitam";
    private static final String BAD_REQUEST_EXCEPTION_MSG = "Bad request Exception";
    private static final String ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Access client unavailable";
    private static final String ACCESS_SERVER_EXCEPTION_MSG = "Access Server exception";
    private static final String INTERNAL_SERVER_ERROR_MSG = "INTERNAL SERVER ERROR";
    private static final String SEARCH_CRITERIA_MANDATORY_MSG = "Search criteria payload is mandatory";
    private static final String UPDATE_RULES_KEY = "UpdatedRules";
    private static final String FIELD_ID_KEY = "fieldId";
    private static final String NEW_FIELD_VALUE_KEY = "newFieldValue";
    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";

    private static final String LOGBOOK_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Logbook Client NOT FOUND Exception";
    private static final ConcurrentMap<String, List<Object>> uploadRequestsStatus = new ConcurrentHashMap<>();
    private static final int GUID_INDEX = 0;

    private Map<String, AtomicLong> uploadMap = new HashMap<>();
    private ExecutorService threadPoolExecutor = Executors.newCachedThreadPool(VitamThreadFactory.getInstance());

    private final UserInterfaceTransactionManager userInterfaceTransactionManager;
    private final DslQueryHelper dslQueryHelper;
    private final PaginationHelper paginationHelper;
    private final IngestExternalClientFactory ingestExternalClientFactory;
    private final AdminExternalClientFactory adminExternalClientFactory;
    private final Set<String> permissions;
    private final List<String> secureMode;
    private final List<MediaType> allowedToVisualizeMediaTypes;

    /**
     * Constructor
     *
     * @param permissions
     * @param webApplicationConfig the web server ihm-demo configuration
     */
    public WebApplicationResource(Set<String> permissions, WebApplicationConfig webApplicationConfig) {
        super(new BasicVitamStatusServiceImpl());
        this.permissions = permissions;
        this.ingestExternalClientFactory = IngestExternalClientFactory.getInstance();
        this.adminExternalClientFactory = AdminExternalClientFactory.getInstance();
        this.userInterfaceTransactionManager = UserInterfaceTransactionManager.getInstance();
        this.secureMode = webApplicationConfig.getSecureMode();
        this.allowedToVisualizeMediaTypes = webApplicationConfig.getAllowedMediaTypes();
        this.dslQueryHelper = DslQueryHelper.getInstance();
        this.paginationHelper = PaginationHelper.getInstance();
    }

    @VisibleForTesting
    public WebApplicationResource(
        Set<String> permissions,
        WebApplicationConfig webApplicationConfig,
        IngestExternalClientFactory ingestExternalClientFactory,
        AdminExternalClientFactory adminExternalClientFactory,
        UserInterfaceTransactionManager userInterfaceTransactionManager,
        DslQueryHelper dslQueryHelper,
        PaginationHelper paginationHelper
    ) {
        super(new BasicVitamStatusServiceImpl());
        this.permissions = permissions;
        this.ingestExternalClientFactory = ingestExternalClientFactory;
        this.adminExternalClientFactory = adminExternalClientFactory;
        this.userInterfaceTransactionManager = userInterfaceTransactionManager;
        this.secureMode = webApplicationConfig.getSecureMode();
        this.allowedToVisualizeMediaTypes = webApplicationConfig.getAllowedMediaTypes();
        this.dslQueryHelper = dslQueryHelper;
        this.paginationHelper = paginationHelper;
    }

    /**
     * Retrieve all the messages for logbook
     *
     * @return Response
     */
    @GET
    @Path("/messages/logbook")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("messages:logbook:read")
    public Response getLogbookMessages() {
        // TODO P0 : If translation key could be the same in different
        // .properties file, MUST add an unique prefix per
        // file
        return Response.status(Status.OK).entity(VitamLogbookMessages.getAllMessages()).build();
    }

    /**
     * @param request needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param criteria criteria search for units
     * @return Reponse
     */
    @POST
    @Path("/archivesearch/units")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archivesearch:units:read")
    public Response getArchiveSearchResult(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String criteria
    ) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        String requestId;
        RequestResponse result;
        OffsetBasedPagination pagination;
        try {
            Enumeration<String> headersReqId = request.getHeaders(IhmWebAppHeader.REQUEST_ID.name());
            while (headersReqId.hasMoreElements()) {
                SanityChecker.checkParameter(headersReqId.nextElement());
            }
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
                result = RequestResponseOK.getFromJsonNode(paginationHelper.getResult(sessionId, pagination));

                if (!result.isOk()) {
                    return result.toResponse();
                }

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
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(criteria));

                final Map<String, Object> criteriaMap = JsonHandler.getMapFromString(criteria);
                final JsonNode preparedQueryDsl = dslQueryHelper.createSelectElasticsearchDSLQuery(criteriaMap);

                result = userInterfaceTransactionManager.searchUnits(
                    preparedQueryDsl,
                    userInterfaceTransactionManager.getVitamContext(request)
                );

                if (!result.isOk()) {
                    return result.toResponse();
                }

                // save result
                paginationHelper.setResult(sessionId, result.toJsonNode());
                // pagination
                result = RequestResponseOK.getFromJsonNode(paginationHelper.getResult(result.toJsonNode(), pagination));

                return Response.status(Status.OK).entity(result).build();
            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
                return Response.status(Status.BAD_REQUEST).build();
            } catch (final AccessExternalClientNotFoundException e) {
                LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
                return Response.status(Status.NOT_FOUND).build();
            } catch (final AccessUnauthorizedException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                return Response.status(Status.UNAUTHORIZED).build();
            } catch (final VitamClientException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    /**
     * @param request needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param unitId archive unit id
     * @return archive unit details
     */
    @GET
    @Path("/archivesearch/unit/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archivesearch:units:read")
    public Response getArchiveUnitDetails(@Context HttpServletRequest request, @PathParam("id") String unitId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
            // Prepare required map
            final Map<String, String> selectUnitIdMap = new HashMap<>();
            selectUnitIdMap.put(dslQueryHelper.PROJECTION_DSL, BuilderToken.GLOBAL.RULES.exactToken());
            JsonNode preparedQueryDsl = dslQueryHelper.createGetByIdDSLSelectMultipleQuery(selectUnitIdMap);
            final RequestResponse<JsonNode> archiveDetails = userInterfaceTransactionManager.getArchiveUnitDetails(
                preparedQueryDsl,
                unitId,
                userInterfaceTransactionManager.getVitamContext(request)
            );
            return archiveDetails.toResponse();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param request needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param unitId archive unit id
     * @return archive unit details
     */
    @GET
    @Path("/archivesearch/unitsWithInheritedRules/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archivesearch:units:read")
    public Response getArchiveUnitDetailsWithInheritance(
        @Context HttpServletRequest request,
        @PathParam("id") String unitId
    ) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));

            final SelectMultiQuery select = new SelectMultiQuery();
            select.addProjection(JsonHandler.createObjectNode());
            if (unitId != null) {
                select.setQuery(eq(VitamFieldsHelper.id(), unitId));
            }
            ObjectNode preparedQueryDsl = select.getFinalSelect();

            final RequestResponse<JsonNode> archiveDetails =
                userInterfaceTransactionManager.selectUnitsWithInheritedRules(
                    preparedQueryDsl,
                    userInterfaceTransactionManager.getVitamContext(request)
                );

            return archiveDetails.toResponse();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param request the http request
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/logbook/operations")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("logbook:operations:read")
    public Response getLogbookResult(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        ParametersChecker.checkParameter("cookie is mandatory", sessionId);
        String requestId;
        RequestResponse result;
        OffsetBasedPagination pagination;

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
        Integer tenantId = userInterfaceTransactionManager.getTenantId(request);
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
            requestId = GUIDFactory.newRequestIdGUID(tenantId).toString();

            try {
                ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
                final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

                result = userInterfaceTransactionManager.selectOperation(
                    query,
                    userInterfaceTransactionManager.getVitamContext(request)
                );

                if (!result.isOk()) {
                    return result.toResponse();
                }
                // save result
                paginationHelper.setResult(sessionId, result.toJsonNode());
                // pagination
                result = RequestResponseOK.getFromJsonNode(paginationHelper.getResult(result.toJsonNode(), pagination));
            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
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

    @POST
    @Path("/logbook/operations/last")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("logbook:operations:read")
    public Response getLogbookResultLast(@Context HttpServletRequest request, JsonNode query) {
        RequestResponse<LogbookOperation> result;
        try {
            SanityChecker.checkJsonAll(query);
            result = userInterfaceTransactionManager.selectOperation(
                query,
                userInterfaceTransactionManager.getVitamContext(request)
            );
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Vitam Client Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * @param request needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param operationId id of operation
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/logbook/operations/{idOperation}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("logbook:operations:read")
    public Response getLogbookResultById(
        @Context HttpServletRequest request,
        @PathParam("idOperation") String operationId,
        String options
    ) {
        RequestResponse<LogbookOperation> result;
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = userInterfaceTransactionManager.selectOperationbyId(
                operationId,
                userInterfaceTransactionManager.getVitamContext(request)
            );
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Vitam Client Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * upload : API Endpoint that can Handle chunk mode. Chunks information are given in header (Fast catch of these
     * header are present in the code) <br />
     * The front should give some information
     * <ul>
     * <li>Flow-Chunk-Number =&gt; The index of the current chunk</li>
     * <li>Flow-Chunk-Size =&gt; The configured maximal size of a chunk</li>
     * <li>Flow-Current-Chunk-Size =&gt; The size of the current chunk</li>
     * <li>Flow-Total-Size =&gt; The total size of the file (All chunks)</li>
     * <li>Flow-Identifier =&gt; The identifier of the flow</li>
     * <li>Flow-Filename =&gt; The file name</li>
     * <li>Flow-Relative-Path =&gt; (?)The relative path (or the file name only)</li>
     * <li>Flow-Total-Chunks =&gt; The number of chunks</li>
     * </ul>
     *
     * @param request the http servlet request
     * @param response the http servlet response
     * @param stream data input stream for the current chunk
     * @return Response
     */
    @Path("ingest/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("ingest:create")
    public Response ingest(@Context HttpServletResponse response, @Context HttpServletRequest request, byte[] stream) {
        String operationGuid;
        String chunkOffset = request.getHeader(X_CHUNK_OFFSET);
        String chunkSizeTotal = request.getHeader(X_SIZE_TOTAL);
        String contextId = request.getHeader(GlobalDataRest.X_CONTEXT_ID);
        String action = request.getHeader(GlobalDataRest.X_ACTION);
        Integer tenantId = userInterfaceTransactionManager.getTenantId(request);

        if (
            request.getHeader(GlobalDataRest.X_REQUEST_ID) == null ||
            request.getHeader(GlobalDataRest.X_REQUEST_ID).isEmpty()
        ) {
            // GUID operation (Server Application level)
            operationGuid = GUIDFactory.newGUID().getId();
            AtomicLong writtenByteSize = new AtomicLong(0);
            uploadMap.put(operationGuid, writtenByteSize);
        } else {
            operationGuid = request.getHeader(GlobalDataRest.X_REQUEST_ID);
            try {
                SanityChecker.checkParameter(operationGuid);
            } catch (InvalidParseOperationException e) {
                LOGGER.error("Bad request exception", e);
                return Response.status(Status.BAD_REQUEST).build();
            }
        }
        FileChannel fileChannel = null;
        try (
            RandomAccessFile randomAccessFile = new RandomAccessFile(
                PropertiesUtils.fileFromTmpFolder(operationGuid).getAbsolutePath(),
                "rw"
            )
        ) {
            fileChannel = randomAccessFile.getChannel();
            long offset = Long.parseLong(chunkOffset);
            int writtenByte = fileChannel.write(ByteBuffer.wrap(stream), offset);
            AtomicLong writtenByteSize = uploadMap.get(operationGuid);
            long total = writtenByteSize.addAndGet(writtenByte);
            long size = Long.parseLong(chunkSizeTotal);
            if (total >= size) {
                fileChannel.force(false);
                startUpload(operationGuid, tenantId, contextId, action);
                uploadMap.remove(operationGuid);
            }
            return Response.status(Status.OK)
                .entity(
                    JsonHandler.getFromString(
                        "{\"" + GlobalDataRest.X_REQUEST_ID.toLowerCase() + "\":\"" + operationGuid + "\"}"
                    )
                )
                .header(GlobalDataRest.X_REQUEST_ID, operationGuid)
                .build();
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("Upload failed", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Upload failed", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .header(GlobalDataRest.X_REQUEST_ID, operationGuid)
                .build();
        } finally {
            try {
                if (fileChannel != null) {
                    fileChannel.close();
                }
            } catch (IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    private void startUpload(String operationGUID, Integer tenantId, String contextId, String action) {
        final IngestThread ingestThread = new IngestThread(operationGUID, tenantId, contextId, action);
        ingestThread.start();
    }

    class IngestThread extends Thread {

        String operationGuidFirstLevel;
        Integer tenantId;
        String contextId;
        String action;

        IngestThread(String operationGuidFirstLevel, Integer tenantId, String contextId, String action) {
            this.operationGuidFirstLevel = operationGuidFirstLevel;
            this.tenantId = tenantId;
            this.contextId = contextId;
            this.action = action;
        }

        @Override
        public void run() {
            // start the upload
            final File temporarSipFile = PropertiesUtils.fileFromTmpFolder(operationGuidFirstLevel);

            try (
                IngestExternalClient client = ingestExternalClientFactory.getClient();
                InputStream fileInputStream = new FileInputStream(temporarSipFile)
            ) {
                final RequestResponse<Void> finalResponse = client.ingest(
                    new VitamContext(tenantId).setApplicationSessionId(
                        userInterfaceTransactionManager.getAppSessionId()
                    ),
                    fileInputStream,
                    contextId,
                    action
                );

                int responseStatus = finalResponse.getHttpCode();
                final String guid = finalResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
                final List<Object> finalResponseDetails = new ArrayList<>();
                finalResponseDetails.add(guid);
                finalResponseDetails.add(Status.fromStatusCode(responseStatus));
                if (Status.SERVICE_UNAVAILABLE.getStatusCode() == finalResponse.getHttpCode()) {
                    final String responseString = ((VitamError) finalResponse).getMessage();
                    if (responseString != null) {
                        finalResponseDetails.add(responseString);
                    }
                }
                uploadRequestsStatus.put(operationGuidFirstLevel, finalResponseDetails);
            } catch (IOException | VitamException e) {
                LOGGER.error("Upload failed", e);
                final List<Object> finalResponseDetails = new ArrayList<>();
                finalResponseDetails.add(operationGuidFirstLevel);
                finalResponseDetails.add(Status.INTERNAL_SERVER_ERROR);
                uploadRequestsStatus.put(operationGuidFirstLevel, finalResponseDetails);
            } finally {
                try {
                    Files.delete(temporarSipFile.toPath());
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

    /**
     * Check if the upload operation is done
     *
     * @param operationId
     * @return the Response
     */
    @Path("check/{id_op}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("check:read")
    public Response checkUploadOperation(
        @PathParam("id_op") String operationId,
        @Context HttpServletRequest request,
        @QueryParam("action") String action
    ) throws VitamClientException, IngestExternalException {
        // TODO Need a tenantId test for checking upload (Only IHM-DEMO scope,
        // dont call VITAM backend) ?
        // 1- Check if the requested operation is done
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        // mapping X-Request-Id
        final int tenantId = userInterfaceTransactionManager.getTenantId(request);
        final List<Object> responseDetails = uploadRequestsStatus.get(operationId);
        if (
            responseDetails != null &&
            responseDetails.size() >= 3 &&
            responseDetails.get(1).equals(Status.SERVICE_UNAVAILABLE)
        ) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(((String) responseDetails.get(2)).getBytes(CharsetUtils.UTF8))
                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header(CONTENT_DISPOSITION, "attachment; filename=ATR_" + operationId + ".xml")
                .header(GlobalDataRest.X_REQUEST_ID, operationId)
                .build();
        }

        if (responseDetails != null) {
            try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
                String id = responseDetails.get(GUID_INDEX).toString();

                final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(client);
                if (vitamPoolingClient.wait(tenantId, id, 30, 1000L, TimeUnit.MILLISECONDS)) {
                    final RequestResponse<ItemStatus> requestResponse = client.getOperationProcessExecutionDetails(
                        userInterfaceTransactionManager.getVitamContext(request),
                        id
                    );
                    if (requestResponse.isOk()) {
                        ItemStatus itemStatus = ((RequestResponseOK<ItemStatus>) requestResponse).getResults().get(0);
                        if (ProcessState.COMPLETED.equals(itemStatus.getGlobalState())) {
                            File file = downloadAndSaveATR(id, request);

                            if (file != null) {
                                LogbookEventOperation lastEvent = getlogBookOperationStatus(id, request);
                                // ingestExternalClient client
                                int status = getStatus(lastEvent);
                                return Response.status(status)
                                    .entity(new FileInputStream(file))
                                    .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                                    .header(CONTENT_DISPOSITION, "attachment; filename=ATR_" + id + ".xml")
                                    .header(GlobalDataRest.X_REQUEST_ID, operationId)
                                    .build();
                            }
                        } else if (ProcessState.PAUSE.equals(itemStatus.getGlobalState())) {
                            return Response.status(itemStatus.getGlobalStatus().getEquivalentHttpStatus()).build();
                        } else {
                            return Response.status(Status.NO_CONTENT)
                                .header(GlobalDataRest.X_REQUEST_ID, operationId)
                                .build();
                        }
                    } else {
                        return requestResponse.toResponse();
                    }
                } else {
                    return Response.status(Status.NO_CONTENT).header(GlobalDataRest.X_REQUEST_ID, operationId).build();
                }
            } catch (VitamException e) {
                LOGGER.error(e);
                if (null != e.getVitamError()) {
                    return e.getVitamError().toResponse();
                } else {
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .header(GlobalDataRest.X_REQUEST_ID, operationId)
                        .entity(e)
                        .build();
                }
            } catch (Exception e) {
                LOGGER.error(e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .header(GlobalDataRest.X_REQUEST_ID, operationId)
                    .entity(e)
                    .build();
            }
        }
        // 2- Return the created GUID
        return Response.status(Status.NO_CONTENT).header(GlobalDataRest.X_REQUEST_ID, operationId).build();
    }

    private LogbookEventOperation getlogBookOperationStatus(String operationId, HttpServletRequest request)
        throws VitamClientException {
        final RequestResponse<LogbookOperation> result = userInterfaceTransactionManager.selectOperationbyId(
            operationId,
            userInterfaceTransactionManager.getVitamContext(request)
        );
        RequestResponseOK<LogbookOperation> responseOK = (RequestResponseOK<LogbookOperation>) result;
        List<LogbookOperation> results = responseOK.getResults();
        LogbookOperation operation = results.get(0);

        return Iterables.getLast(operation.getEvents());
    }

    private static int getStatus(LogbookEventOperation lastEvent) throws VitamException {
        if (lastEvent.getOutcome() == null) {
            throw new VitamException("parsing Error");
        }
        switch (lastEvent.getOutcome()) {
            case "WARNING":
                return 206;
            case "OK":
                return 200;
            case "KO":
                return 400;
            default:
                return 500;
        }
    }

    /**
     * Start elimination analysis
     *
     * @param request HTTP request
     * @param updateSet contains updated field
     * @return archive unit details
     */
    @POST
    @Path("/elimination/analysis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("elimination:analysis")
    public Response startEliminationAnalysis(@Context HttpServletRequest request, String updateSet) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateSet));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try {
            EliminationRequestBody requestBody = getEliminationRequestBody(updateSet);

            final RequestResponse<JsonNode> eliminationResponse =
                userInterfaceTransactionManager.startEliminationAnalysis(
                    requestBody,
                    userInterfaceTransactionManager.getVitamContext(request)
                );
            return eliminationResponse.toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Start elimination action
     *
     * @param request HTTP request
     * @param updateSet contains updated field
     * @return archive unit details
     */
    @POST
    @Path("/elimination/action")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("elimination:action")
    public Response startEliminationAction(@Context HttpServletRequest request, String updateSet) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateSet));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try {
            // Parse updateSet
            EliminationRequestBody requestBody = getEliminationRequestBody(updateSet);

            final RequestResponse<JsonNode> eliminationResponse =
                userInterfaceTransactionManager.startEliminationAction(
                    requestBody,
                    userInterfaceTransactionManager.getVitamContext(request)
                );
            return eliminationResponse.toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private EliminationRequestBody getEliminationRequestBody(String updateSet) throws InvalidParseOperationException {
        // Parse updateSet
        final JsonNode updateJsonNode = JsonHandler.getFromString(updateSet);
        ObjectNode query = JsonHandler.createObjectNode();
        query.set(BuilderToken.GLOBAL.ROOTS.exactToken(), JsonHandler.createArrayNode());
        query.set(
            BuilderToken.GLOBAL.QUERY.exactToken(),
            updateJsonNode.get("query").get(BuilderToken.GLOBAL.QUERY.exactToken())
        );
        if (updateJsonNode.has("threshold") && !updateJsonNode.get("threshold").isNull()) {
            if (!updateJsonNode.get("threshold").isIntegralNumber()) {
                throw new InvalidParseOperationException("invalid request");
            }
            query.put(BuilderToken.GLOBAL.THRESOLD.exactToken(), updateJsonNode.get("threshold").longValue());
        }
        EliminationRequestBody requestBody = new EliminationRequestBody();
        requestBody.setDate(updateJsonNode.get("date").asText());
        requestBody.setDslRequest(query);
        return requestBody;
    }

    /**
     * Masive archive units update
     *
     * @param request HTTP request
     * @param updateSet contains updated field
     * @return archive unit details
     */
    @POST
    @Path("/archiveupdate/units")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveupdate:units:update")
    public Response massiveArchiveUnitUpdate(@Context HttpServletRequest request, String updateSet) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateSet));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try {
            // Parse updateSet
            final JsonNode modifiedFields = JsonHandler.getFromString(updateSet);

            // get Update model for Metadata
            final ObjectNode baseQuery = dslQueryHelper.createMassiveUpdateDSLBaseQuery(modifiedFields);
            final JsonNode rulesQuery = modifiedFields.get("rulesUpdates");

            RequestResponse<JsonNode> metadataUpdateResponse = null;
            RequestResponse<JsonNode> rulesUpdateResponse = null;

            UpdateMultiQuery metadataUpdate = dslQueryHelper.getFullMetadataActionQuery(
                modifiedFields.get("metadataUpdates")
            );
            ObjectNode metadataQuery = baseQuery.deepCopy();

            if (metadataUpdate != null) {
                metadataQuery.set(
                    BuilderToken.GLOBAL.ACTION.exactToken(),
                    metadataUpdate.getFinalUpdate().get(BuilderToken.GLOBAL.ACTION.exactToken())
                );
                metadataUpdateResponse = userInterfaceTransactionManager.massiveUnitsUpdate(
                    metadataQuery,
                    userInterfaceTransactionManager.getVitamContext(request)
                );
            }

            if (rulesQuery != null) {
                baseQuery.remove("$action");

                ObjectNode fullRuleQuery = JsonHandler.createObjectNode();
                fullRuleQuery.set("dslRequest", baseQuery);
                fullRuleQuery.set("ruleActions", rulesQuery);

                rulesUpdateResponse = userInterfaceTransactionManager.massiveRulesUpdate(
                    fullRuleQuery,
                    userInterfaceTransactionManager.getVitamContext(request)
                );
            }

            // FIXME: What should be returned ? metadataUpdateResponse + rulesUpdateResponse ?
            return metadataUpdateResponse == null
                ? rulesUpdateResponse == null
                    ? Response.status(Status.BAD_REQUEST).build()
                    : rulesUpdateResponse.toResponse()
                : metadataUpdateResponse.toResponse();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update Archive Units
     *
     * @param request HTTP request
     * @param updateSet contains updated field
     * @param unitId archive unit id
     * @return archive unit details
     */
    @POST
    @Path("/archiveupdate/units/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveupdate:units:update")
    public Response updateArchiveUnitDetails(
        @Context HttpServletRequest request,
        @PathParam("id") String unitId,
        String updateSet
    ) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateSet));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try {
            // Parse updateSet
            final Map<String, JsonNode> updateUnitIdMap = new HashMap<>();
            final Map<String, JsonNode> updateRules = new HashMap<>();
            final JsonNode modifiedFields = JsonHandler.getFromString(updateSet);
            if (modifiedFields != null && modifiedFields.isArray()) {
                for (final JsonNode modifiedField : modifiedFields) {
                    if (modifiedField.get(UPDATE_RULES_KEY) != null) {
                        ArrayNode rulesCategories = (ArrayNode) modifiedField.get(UPDATE_RULES_KEY);
                        for (JsonNode ruleCategory : rulesCategories) {
                            for (String categoryKey : VitamConstants.getSupportedRules()) {
                                JsonNode rules = ruleCategory.get(categoryKey);
                                if (rules != null) {
                                    updateRules.put(categoryKey, rules);
                                }
                            }
                        }
                    } else {
                        updateUnitIdMap.put(
                            modifiedField.get(FIELD_ID_KEY).textValue(),
                            modifiedField.get(NEW_FIELD_VALUE_KEY)
                        );
                    }
                }
            }

            // Add ID to set root part
            final JsonNode preparedQueryDsl = dslQueryHelper.createUpdateByIdDSLQuery(updateUnitIdMap, updateRules);
            final RequestResponse<JsonNode> archiveDetails = userInterfaceTransactionManager.updateUnits(
                preparedQueryDsl,
                unitId,
                userInterfaceTransactionManager.getVitamContext(request)
            );
            return archiveDetails.toResponse();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param request HTTP request
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/formats")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:formats:read")
    public Response getFileFormats(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        // FIXME P0: Pagination rollbacked because of error on mongo/ES indexation --> use the commented method after
        // some fixes
        // FIXME Pagination should be use as in others endpoints after solution found (See Item #2227)
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);
            final RequestResponse<FileFormatModel> result = adminClient.findFormats(
                userInterfaceTransactionManager.getVitamContext(request),
                query
            );
            if (result != null && result instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(result).build();
            }
            if (result != null && result instanceof VitamError) {
                LOGGER.error(result.toString());
                return Response.status(result.getHttpCode()).entity(result).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param request HTTP request
     * @param formatId id of format
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/formats/{idFormat:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:formats:read")
    public Response getFormatById(
        @Context HttpServletRequest request,
        @PathParam("idFormat") String formatId,
        String options
    ) {
        RequestResponse<FileFormatModel> result;

        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            ParametersChecker.checkParameter("Format Id is mandatory", formatId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
            result = adminClient.findFormatById(userInterfaceTransactionManager.getVitamContext(request), formatId);
            if (result != null && result instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(result).build();
            }
            if (result != null && result instanceof VitamError) {
                LOGGER.error(result.toString());
                return Response.status(result.getHttpCode()).entity(result).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /***
     * check the referential format
     *
     * @param request HTTP request
     * @param input the format file xml
     * @return If the formet is valid, return ok. If not, return the list of errors
     */
    @POST
    @Path("/format/check")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("format:check")
    public Response checkRefFormat(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            Response response = adminClient.checkFormats(
                userInterfaceTransactionManager.getVitamContext(request),
                input
            );
            return response;
        } catch (final VitamClientException e) {
            LOGGER.error("VitamClientException ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    /**
     * Upload the referential format in the base
     *
     * @param request HTTP request
     * @param input the format file xml
     * @return Response
     */
    @POST
    @Path("/format/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("format:create")
    public Response uploadRefFormat(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createFormats(
                userInterfaceTransactionManager.getVitamContext(request),
                input,
                request.getHeader(GlobalDataRest.X_FILENAME)
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    /**
     * Retrieve an ObjectGroup as Json data based on the provided ObjectGroup id
     *
     * @param request HTTP request
     * @param objectGroupId the object group Id
     * @return a response containing a json with informations about usages and versions for an object group
     */
    @GET
    @Path("/archiveunit/objects/{idOG}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveunit:objects:read")
    public Response getArchiveObjectGroup(
        @Context HttpServletRequest request,
        @PathParam("idOG") String objectGroupId
    ) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));

            SelectMultiQuery select = new SelectMultiQuery();
            select.addUsedProjection("#qualifiers");
            final JsonNode preparedQueryDsl = select.getFinalSelectById();
            final RequestResponse<JsonNode> searchResult = userInterfaceTransactionManager.selectObjectbyId(
                preparedQueryDsl,
                objectGroupId,
                userInterfaceTransactionManager.getVitamContext(request)
            );

            if (searchResult.isOk()) {
                return Response.status(searchResult.getStatus())
                    .entity(JsonTransformer.transformResultObjects(searchResult.toJsonNode()))
                    .build();
            } else {
                return Response.status(searchResult.getStatus()).entity(searchResult.toJsonNode()).build();
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieve an Object data as an input stream. Download by access.
     *
     * @param unitId the unit Id
     * @param usage additional mandatory parameters usage
     * @param filename additional mandatory parameters filename
     * @param tenantId the tenant id
     * @param contractId the contract id
     * @param asyncResponse will return the inputstream
     */
    @GET
    @Path("/archiveunit/objects/download/{unitId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("archiveunit:objects:read")
    public void getObjectAsInputStreamAsync(
        @PathParam("unitId") String unitId,
        @QueryParam("usage") String usage,
        @QueryParam("filename") String filename,
        @QueryParam("tenantId") Integer tenantId,
        @QueryParam("contractId") String contractId,
        @Suspended final AsyncResponse asyncResponse
    ) {
        threadPoolExecutor.execute(
            () -> asyncGetObjectStream(asyncResponse, unitId, usage, filename, tenantId, contractId)
        );
    }

    /**
     * Retrieve an Object data stored by ingest operation as an input stream. Download by ingests.
     *
     * @param request HTTP request
     * @param objectId the object id to get
     * @param type of collection
     * @param asyncResponse request asynchronized response
     */
    @GET
    @Path("/ingests/{idObject}/{type}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("ingests:read")
    public void getObjectFromStorageAsInputStreamAsync(
        @Context HttpServletRequest request,
        @PathParam("idObject") String objectId,
        @PathParam("type") String type,
        @Suspended final AsyncResponse asyncResponse
    ) {
        Integer tenantId = userInterfaceTransactionManager.getTenantId(request);
        String contractId = userInterfaceTransactionManager.getContractId(request);
        String personalCert = userInterfaceTransactionManager.getPersonalCertificate(request);
        threadPoolExecutor.execute(
            () -> asyncGetObjectStorageStream(asyncResponse, objectId, type, tenantId, contractId, personalCert)
        );
    }

    private void asyncGetObjectStorageStream(
        AsyncResponse asyncResponse,
        String objectId,
        String type,
        Integer tenantId,
        String contractId,
        String personalCert
    ) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(type));
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectId);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, type);
        } catch (final InvalidParseOperationException exc) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
            return;
        } catch (final IllegalArgumentException exc) {
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.PRECONDITION_FAILED).build()
            );
            return;
        }
        try (IngestExternalClient client = ingestExternalClientFactory.getClient()) {
            IngestCollection collection = IngestCollection.valueOf(type.toUpperCase());
            Response response = client.downloadObjectAsync(
                userInterfaceTransactionManager.getVitamContext(tenantId, contractId, personalCert),
                objectId,
                collection
            );
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                helper.writeResponse(Response.ok().header(CONTENT_DISPOSITION, "filename=" + objectId + ".xml"));
            } else {
                helper.writeResponse(Response.status(response.getStatus()));
            }
        } catch (IllegalArgumentException exc) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (final VitamClientException exc) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build()
            );
        }
    }

    private void asyncGetObjectStream(
        AsyncResponse asyncResponse,
        String unitId,
        String usage,
        String filename,
        Integer tenantId,
        String contractId
    ) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(usage));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(filename));
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, usage);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, filename);
        } catch (final InvalidParseOperationException exc) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
            return;
        } catch (final IllegalArgumentException exc) {
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.PRECONDITION_FAILED).build()
            );
            return;
        }
        try {
            String[] usageAndVersion = usage.split("_");
            if (usageAndVersion.length != 2) {
                throw new InvalidParameterException();
            }
            userInterfaceTransactionManager.getObjectAsInputStream(
                asyncResponse,
                unitId,
                usageAndVersion[0],
                Integer.parseInt(usageAndVersion[1]),
                filename,
                new VitamContext(tenantId)
                    .setAccessContract(contractId)
                    .setApplicationSessionId(userInterfaceTransactionManager.getAppSessionId()),
                this.allowedToVisualizeMediaTypes
            );
        } catch (final VitamClientException exc) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build()
            );
        } catch (final Exception exc) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build()
            );
        }
    }

    /***** rules Management ************/

    /**
     * @param request HTTP request
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:rules:read")
    public Response getFileRules(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        ParametersChecker.checkParameter("cookie is mandatory", sessionId);

        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);
            RequestResponse result = adminClient.findRules(
                userInterfaceTransactionManager.getVitamContext(request),
                query
            );
            if (result != null && result instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(result).build();
            }
            if (result != null && result instanceof VitamError) {
                LOGGER.error(result.toString());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(result).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.OK)
                .entity(new RequestResponseOK<>().setHttpCode(Status.OK.getStatusCode()))
                .build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param request HTTP request
     * @param ruleId id of rule
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/rules/{id_rule}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:rules:read")
    public Response getRuleById(
        @Context HttpServletRequest request,
        @PathParam("id_rule") String ruleId,
        String options
    ) {
        RequestResponse<FileRulesModel> result = null;

        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            ParametersChecker.checkParameter("rule Id is mandatory", ruleId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(ruleId));
            result = adminClient.findRuleById(userInterfaceTransactionManager.getVitamContext(request), ruleId);
            if (result != null && result instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(result).build();
            }
            if (result != null && result instanceof VitamError) {
                LOGGER.error(result.toString());
                return Response.status(result.getHttpCode()).entity(result).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/rules/report/download/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadRulesReport(@Context HttpServletRequest request, @PathParam("id") String id) {
        try {
            File file = downloadReportOrCsv(id, request, JSON);
            if (file != null) {
                return Response.ok()
                    .entity(new FileInputStream(file))
                    .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header(CONTENT_DISPOSITION, "attachment; filename=" + id + ".json")
                    .build();
            } else {
                return Response.status(Status.NO_CONTENT).build();
            }
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/report/distribution/download/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDistributionReport(@Context HttpServletRequest request, @PathParam("id") String id) {
        try {
            SanityChecker.checkParameter(id);
            File file = downloadReportOrCsv(id, request, DISTRIBUTION);
            if (file != null) {
                return Response.ok()
                    .entity(new FileInputStream(file))
                    .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header(CONTENT_DISPOSITION, "attachment; filename=" + id + ".json")
                    .build();
            } else {
                return Response.status(Status.NOT_FOUND).build();
            }
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/report/batchreport/download/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadBatchReport(@Context HttpServletRequest request, @PathParam("id") String id) {
        try {
            SanityChecker.checkParameter(id);
            File file = downloadReportOrCsv(id, request, BATCH_REPORT);
            if (file != null) {
                return Response.ok()
                    .entity(new FileInputStream(file))
                    .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header(CONTENT_DISPOSITION, "attachment; filename=" + id + ".jsonl")
                    .build();
            } else {
                return Response.status(Status.NOT_FOUND).build();
            }
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/referential/download/{id}/{type}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadCsvReferential(
        @Context HttpServletRequest request,
        @PathParam("id") String id,
        @PathParam("type") String type
    ) {
        try {
            File file = downloadReportOrCsv(id, request, type);
            if (file != null) {
                return Response.ok()
                    .entity(new FileInputStream(file))
                    .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header(CONTENT_DISPOSITION, "attachment; filename=" + id + CSV)
                    .build();
            } else {
                return Response.status(Status.NO_CONTENT).build();
            }
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    private File downloadReportOrCsv(String guid, HttpServletRequest request, String typeOfDownload)
        throws VitamClientException {
        File file = null;
        Response response = null;
        try (AdminExternalClient adminExternalClient = adminExternalClientFactory.getClient()) {
            if (DISTRIBUTION.equals(typeOfDownload)) {
                response = adminExternalClient.downloadDistributionReport(
                    userInterfaceTransactionManager.getVitamContext(request),
                    guid
                );
                file = getFileFromResponse(response.readEntity(InputStream.class), guid, JSON);
            }
            if (BATCH_REPORT.equals(typeOfDownload)) {
                response = adminExternalClient.downloadBatchReport(
                    userInterfaceTransactionManager.getVitamContext(request),
                    guid
                );
                file = getFileFromResponse(response.readEntity(InputStream.class), guid, JSONL);
            }
            if (JSON.equals(typeOfDownload)) {
                response = adminExternalClient.downloadRulesReport(
                    userInterfaceTransactionManager.getVitamContext(request),
                    guid
                );
                file = getFileFromResponse(response.readEntity(InputStream.class), guid, JSON);
            } else if (AGENCIES.equals(typeOfDownload)) {
                response = adminExternalClient.downloadAgenciesCsvAsStream(
                    userInterfaceTransactionManager.getVitamContext(request),
                    guid
                );
                file = getFileFromResponse(response.readEntity(InputStream.class), guid, CSV);
            } else if (RULES.equals(typeOfDownload)) {
                response = adminExternalClient.downloadRulesCsvAsStream(
                    userInterfaceTransactionManager.getVitamContext(request),
                    guid
                );
                file = getFileFromResponse(response.readEntity(InputStream.class), guid, CSV);
            }
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(response);
        }
        return file;
    }

    private File getFileFromResponse(InputStream inputStream, String guid, String fileExtension)
        throws VitamClientException {
        File file = null;
        if (inputStream != null) {
            file = PropertiesUtils.fileFromTmpFolder(guid + fileExtension);
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                StreamUtils.copy(inputStream, fileOutputStream);
            } catch (IOException e) {
                throw new VitamClientException("Error during Report generation");
            }
        }
        return file;
    }

    /***
     * check the referential rules
     *
     * @param request HTTP request
     * @param input the rules file csv
     */
    @POST
    @Path("/rules/check")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("rules:create")
    public void checkRefRule(
        @Context HttpServletRequest request,
        InputStream input,
        @Suspended final AsyncResponse asyncResponse
    ) {
        Integer tenantId = userInterfaceTransactionManager.getTenantId(request);
        String contractId = userInterfaceTransactionManager.getContractId(request);
        String personalCert = userInterfaceTransactionManager.getPersonalCertificate(request);
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncDownloadErrorReport(input, asyncResponse, tenantId, contractId, personalCert));
    }

    /**
     * async Download Error Report
     *
     * @param document the input stream to test
     * @param asyncResponse asyncResponse
     * @param tenantId http request
     * @param contractId http request
     */
    private void asyncDownloadErrorReport(
        InputStream document,
        final AsyncResponse asyncResponse,
        Integer tenantId,
        String contractId,
        String personalCert
    ) {
        AsyncInputStreamHelper helper;
        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
            final Response response = client.checkRules(
                userInterfaceTransactionManager.getVitamContext(tenantId, contractId, personalCert),
                document
            );
            helper = new AsyncInputStreamHelper(asyncResponse, response);
            final Response.ResponseBuilder responseBuilder = Response.status(response.getStatus())
                .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME_ERROR_REPORT_JSON)
                .header(CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            helper.writeResponse(responseBuilder);
        } catch (final VitamClientException exc) {
            LOGGER.error(exc.getMessage(), exc);
            asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exc.getMessage()).toString())
                    .build()
            );
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage()).toString())
                    .build()
            );
        }
    }

    /**
     * Upload the referential rules in the base
     *
     * @param request HTTP request
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/rules/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("rules:create")
    public Response uploadRefRule(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createRules(
                userInterfaceTransactionManager.getVitamContext(request),
                input,
                request.getHeader(GlobalDataRest.X_FILENAME)
            );
            if (response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the action registers filtered with option query
     *
     * @param request HTTP request
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/accession-register")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:accession-register:read")
    public Response getAccessionRegister(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        ParametersChecker.checkParameter("cookie is mandatory", sessionId);
        String requestId = null;
        RequestResponse result = null;
        OffsetBasedPagination pagination = null;

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
        Integer tenantId = userInterfaceTransactionManager.getTenantId(request);
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
            requestId = GUIDFactory.newRequestIdGUID(tenantId).toString();

            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            try {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                result = userInterfaceTransactionManager.findAccessionRegisterSummary(
                    options,
                    userInterfaceTransactionManager.getVitamContext(request)
                );

                if (result.isOk()) {
                    // save result
                    paginationHelper.setResult(sessionId, result.toJsonNode());
                    // pagination
                    result = RequestResponseOK.getFromJsonNode(
                        paginationHelper.getResult(result.toJsonNode(), pagination)
                    );
                } else {
                    return Response.status(Status.fromStatusCode(result.getHttpCode())).entity(result).build();
                }
            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).build();
            } catch (final AccessExternalClientNotFoundException e) {
                LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
                return Response.status(Status.NOT_FOUND).build();
            } catch (final AccessUnauthorizedException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                return Response.status(Status.UNAUTHORIZED).build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Status.OK).entity(result).build();
        }
    }

    /**
     * Get the last accession register symbolic
     *
     * @param request HTTP request
     * @param options query criteria
     * @return accession register symbolic
     */
    @POST
    @Path("/admin/accession-register/symbolic")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:accessionregisterssymbolic:read")
    public Response getAccessionRegisterSymbolic(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        try {
            ParametersChecker.checkParameter("cookie is mandatory", sessionId);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));

            final List<String> requestIds = Collections.list(request.getHeaders(IhmWebAppHeader.REQUEST_ID.name()));
            for (String requestId : requestIds) {
                SanityChecker.checkParameter(requestId);
            }

            OffsetBasedPagination pagination = new OffsetBasedPagination(request);
            if (!requestIds.isEmpty()) {
                String requestId = requestIds.get(0);
                return Response.status(Status.OK)
                    .entity(RequestResponseOK.getFromJsonNode(paginationHelper.getResult(sessionId, pagination)))
                    .header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                    .header(IhmDataRest.X_LIMIT, pagination.getLimit())
                    .build();
            }

            VitamContext vitamContext = userInterfaceTransactionManager.getVitamContext(request);
            RequestResponse result = userInterfaceTransactionManager.findAccessionRegisterSymbolic(
                options,
                vitamContext
            );

            if (result.isOk()) {
                JsonNode jsonResult = result.toJsonNode();
                paginationHelper.setResult(sessionId, jsonResult);
                return Response.status(Status.OK)
                    .entity(RequestResponseOK.getFromJsonNode(paginationHelper.getResult(jsonResult, pagination)))
                    .build();
            }

            return Response.status(Status.fromStatusCode(result.getHttpCode())).entity(result).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the detail of an accessionregister matching options query
     *
     * @param request HTTP request
     * @param originatingAgency of accession response to get
     * @param options query criteria
     * @return accession register details
     */
    @POST
    @Path("/admin/accession-register/{originatingAgency}/accession-register-detail")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:accession-register:read")
    public Response getAccessionRegisterDetail(
        @Context HttpServletRequest request,
        @PathParam("originatingAgency") String originatingAgency,
        @CookieParam("JSESSIONID") String sessionId,
        String options
    ) {
        try {
            ParametersChecker.checkParameter("cookie is mandatory", sessionId);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));

            final List<String> requestIds = Collections.list(request.getHeaders(IhmWebAppHeader.REQUEST_ID.name()));
            for (String requestId : requestIds) {
                SanityChecker.checkParameter(requestId);
            }

            OffsetBasedPagination pagination = new OffsetBasedPagination(request);
            if (!requestIds.isEmpty()) {
                String requestId = requestIds.get(0);
                return Response.status(Status.OK)
                    .entity(RequestResponseOK.getFromJsonNode(paginationHelper.getResult(sessionId, pagination)))
                    .header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                    .header(IhmDataRest.X_LIMIT, pagination.getLimit())
                    .build();
            }

            VitamContext vitamContext = userInterfaceTransactionManager.getVitamContext(request);
            RequestResponse result = userInterfaceTransactionManager.findAccessionRegisterDetail(
                originatingAgency,
                options,
                vitamContext
            );

            if (result.isOk()) {
                JsonNode jsonResult = result.toJsonNode();
                paginationHelper.setResult(sessionId, jsonResult);
                return Response.status(Status.OK)
                    .entity(RequestResponseOK.getFromJsonNode(paginationHelper.getResult(jsonResult, pagination)))
                    .build();
            }

            return Response.status(Status.fromStatusCode(result.getHttpCode())).entity(result).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * This resource returns all paths relative to a unit
     *
     * @param request HTTP request
     * @param dslQuery the dsl query
     * @return all paths relative to a unit
     */
    @POST
    @Path("/archiveunit/tree")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveunit:tree:read")
    public Response getUnitTree(@Context HttpServletRequest request, JsonNode dslQuery) {
        try {
            SanityChecker.checkJsonAll(dslQuery);
            final RequestResponse parentsDetails = userInterfaceTransactionManager.searchUnits(
                dslQuery,
                userInterfaceTransactionManager.getVitamContext(request)
            );

            return Response.status(Status.OK).entity(parentsDetails).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param request HTTP request
     * @param object user credentials
     * @return Response OK if login success
     */
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(
        @Context HttpServletRequest request,
        @Context HttpServletRequest httpRequest,
        JsonNode object
    ) {
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
            int timeoutInSeconds = httpRequest.getSession().getMaxInactiveInterval() * 1000;
            // TODO P1 add access log
            LOGGER.info("Login success: " + username);
            List<String> permissionsByUser = PermissionReader.filterPermission(permissions, subject);

            return Response.status(Status.OK)
                .entity(new LoginModel(username, permissionsByUser, timeoutInSeconds, tokenCSRF))
                .build();
        } catch (final Exception uae) {
            LOGGER.debug("Login fail: " + username);
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

    /**
     * @return List of permissions
     */
    @GET
    @Path("permissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPermissions(@Context HttpServletRequest httpRequest) {
        final Subject subject = ThreadContext.getSubject();
        int timeoutInSeconds = httpRequest.getSession().getMaxInactiveInterval() * 1000;
        final String tokenCSRF = XSRFHelper.generateCSRFToken();
        XSRFFilter.addToken(httpRequest.getSession().getId(), tokenCSRF);
        List<String> permissionsByUser = PermissionReader.filterPermission(permissions, subject);
        return Response.status(Status.OK)
            .entity(new LoginModel((String) subject.getPrincipal(), permissionsByUser, timeoutInSeconds, tokenCSRF))
            .build();
    }

    /**
     * returns the unit life cycle based on its id
     *
     * @param request HTTP request
     * @param unitLifeCycleId the unit id (== unit life cycle id)
     * @return the unit life cycle
     */
    @GET
    @Path("/logbookunitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("logbookunitlifecycles:read")
    public Response getUnitLifeCycleById(
        @Context HttpServletRequest request,
        @PathParam("id_lc") String unitLifeCycleId
    ) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitLifeCycleId);
        RequestResponse<LogbookLifecycle> result = null;
        try {
            result = userInterfaceTransactionManager.selectUnitLifeCycleById(
                unitLifeCycleId,
                userInterfaceTransactionManager.getVitamContext(request)
            );
        } catch (final VitamClientException e) {
            LOGGER.error(LOGBOOK_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * returns the object group life cycle based on its id
     *
     * @param request HTTP request
     * @param objectGroupLifeCycleId the object group id (== object group life cycle id)
     * @return the object group life cycle
     */
    @GET
    @Path("/logbookobjectslifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("logbookobjectslifecycles:read")
    public Response getObjectGroupLifeCycleById(
        @Context HttpServletRequest request,
        @PathParam("id_lc") String objectGroupLifeCycleId
    ) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupLifeCycleId);
        RequestResponse<LogbookLifecycle> result = null;

        try {
            result = userInterfaceTransactionManager.selectObjectGroupLifeCycleById(
                objectGroupLifeCycleId,
                userInterfaceTransactionManager.getVitamContext(request)
            );
        } catch (final VitamClientException e) {
            LOGGER.error(LOGBOOK_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * Get the workflow operations list for step by step ingest
     *
     * @param request HTTP request
     * @param query the query
     * @return the operations list
     */
    @POST
    @Path("/operations")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("operations:read")
    public Response listOperationsDetails(@Context HttpServletRequest request, ProcessQuery query) {
        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
            RequestResponse<ProcessDetail> response = client.listOperationsDetails(
                userInterfaceTransactionManager.getVitamContext(request),
                query
            );
            return Response.status(Status.OK).entity(response).build();
        } catch (VitamClientException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update the status of an operation.
     *
     * @param request HTTP request
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @PUT
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("operations:update")
    public Response updateWorkFlowStatus(@Context HttpServletRequest request, @PathParam("id") String id) {
        ParametersChecker.checkParameter("ACTION Request must not be null", request.getHeader(GlobalDataRest.X_ACTION));

        final String xAction = request.getHeader(GlobalDataRest.X_ACTION);
        final int tenantId = userInterfaceTransactionManager.getTenantId(request);
        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
            RequestResponse<ItemStatus> response = client.updateOperationActionProcess(
                userInterfaceTransactionManager.getVitamContext(request),
                xAction,
                id
            );

            if (!response.isOk()) {
                return response.toResponse();
            }

            ItemStatus itemStatusUpdate = ((RequestResponseOK<ItemStatus>) response).getResults().get(0);
            final String globalExecutionState = response.getHeaderString(itemStatusUpdate.getGlobalState().name());
            final String globalExecutionStatus = response.getHeaderString(itemStatusUpdate.getGlobalStatus().name());

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(client);
            if (vitamPoolingClient.wait(tenantId, id, 2000, 3000l, TimeUnit.MILLISECONDS)) {
                final RequestResponse<ItemStatus> requestResponse = client.getOperationProcessExecutionDetails(
                    userInterfaceTransactionManager.getVitamContext(request),
                    id
                );
                if (!requestResponse.isOk()) {
                    return requestResponse.toResponse();
                }
                ItemStatus itemStatus = ((RequestResponseOK<ItemStatus>) requestResponse).getResults().get(0);
                if (ProcessState.COMPLETED.equals(itemStatus.getGlobalState())) {
                    File file = downloadAndSaveATR(id, request);
                    if (file != null) {
                        LogbookEventOperation lastEvent = getlogBookOperationStatus(id, request);
                        // ingestExternalClient client
                        int status = getStatus(lastEvent);
                        return Response.status(status)
                            .entity(new FileInputStream(file))
                            .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                            .header(CONTENT_DISPOSITION, "attachment; filename=ATR_" + id + ".xml")
                            .header(GlobalDataRest.X_REQUEST_ID, id)
                            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, itemStatus.getGlobalState())
                            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, itemStatus.getGlobalStatus())
                            .build();
                    } else {
                        return Response.status(Status.NO_CONTENT).build();
                    }
                } else {
                    return Response.status(itemStatus.getGlobalStatus().getEquivalentHttpStatus())
                        .header(GlobalDataRest.X_REQUEST_ID, id)
                        .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, itemStatus.getGlobalState())
                        .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, itemStatus.getGlobalStatus())
                        .build();
                }
            } else {
                return Response.status(Status.NO_CONTENT)
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, globalExecutionState)
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, globalExecutionStatus)
                    .header(GlobalDataRest.X_REQUEST_ID, id)
                    .build();
            }
        } catch (final ProcessingException e) {
            // if there is an unauthorized action
            LOGGER.error(e);
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/operations/{id}")
    @RequiresPermissions("operations:delete")
    public Response cancelProcess(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<ItemStatus> resp = adminClient.cancelOperationProcessExecution(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (resp.isOk()) {
                return Response.status(Status.OK)
                    .entity(((RequestResponseOK<ItemStatus>) resp).getResults().get(0))
                    .build();
            } else {
                return Response.status(resp.getHttpCode()).entity(((VitamError) resp).getMessage()).build();
            }
        } catch (VitamClientException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (ProcessingException e) {
            LOGGER.error(e);
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
    }

    /**
     * Upload contracts
     *
     * @param request HTTP request
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/contracts")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contracts:create")
    public Response uploadRefContracts(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createIngestContracts(
                userInterfaceTransactionManager.getVitamContext(request),
                input
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets contracts
     *
     * @param request HTTP request
     * @param select the query
     * @return Response
     */
    @POST
    @Path("/contracts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contracts:read")
    public Response findIngestContracts(@Context HttpServletRequest request, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
                RequestResponse<IngestContractModel> response = adminClient.findIngestContracts(
                    userInterfaceTransactionManager.getVitamContext(request),
                    query
                );
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
     * Gets contracts by name
     *
     * @param request HTTP request
     * @param id if of the contract
     * @return Response
     */
    @GET
    @Path("/contracts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contracts:read")
    public Response findContractsById(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<IngestContractModel> response = adminClient.findIngestContractById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getHttpCode()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload Access contracts
     *
     * @param request HTTP request
     * @param contractId the id of ingest contract
     * @return Response
     */
    @POST
    @Path("/contracts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contracts:update")
    public Response updateIngestContracts(
        @Context HttpServletRequest request,
        @PathParam("id") String contractId,
        JsonNode updateOptions
    ) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, contractId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(contractId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateOptions));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            if (!updateOptions.isObject()) {
                throw new InvalidCreateOperationException("Query not valid");
            }
            Update updateRequest = new Update();
            updateRequest.addActions(UpdateActionHelper.set((ObjectNode) updateOptions));
            final RequestResponse archiveDetails = adminClient.updateIngestContract(
                userInterfaceTransactionManager.getVitamContext(request),
                contractId,
                updateRequest.getFinalUpdateById()
            );
            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload Access contracts
     *
     * @param request HTTP request
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/accesscontracts")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("accesscontracts:create")
    public Response uploadAccessContracts(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createAccessContracts(
                userInterfaceTransactionManager.getVitamContext(request),
                input
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
    @RequiresPermissions("accesscontracts:read")
    public Response findAccessContracts(@Context HttpServletRequest request, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
                RequestResponse<AccessContractModel> response = adminClient.findAccessContracts(
                    userInterfaceTransactionManager.getVitamContext(request),
                    query
                );
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
     * Query to Access contracts by id
     *
     * @param request HTTP request
     * @param id of the requested access contract
     * @return Response
     */
    @GET
    @Path("/accesscontracts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("accesscontracts:read")
    public Response findAccessContract(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<AccessContractModel> response = adminClient.findAccessContractById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getHttpCode()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update Access contracts
     *
     * @param request HTTP request
     * @param contractId the id of access contract
     * @return Response
     */
    @POST
    @Path("/accesscontracts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("accesscontracts:update")
    public Response updateAccessContracts(
        @Context HttpServletRequest request,
        @PathParam("id") String contractId,
        JsonNode updateOptions
    ) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, contractId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(contractId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateOptions));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            Update updateRequest = new Update();
            if (!updateOptions.isObject()) {
                throw new InvalidCreateOperationException("Query not valid");
            }
            updateRequest.addActions(UpdateActionHelper.set((ObjectNode) updateOptions));
            final RequestResponse archiveDetails = adminClient.updateAccessContract(
                userInterfaceTransactionManager.getVitamContext(request),
                contractId,
                updateRequest.getFinalUpdateById()
            );
            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload Management contracts
     *
     * @param request HTTP request
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/managementcontracts")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("managementcontracts:create")
    public Response uploadManagementContracts(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createManagementContracts(
                userInterfaceTransactionManager.getVitamContext(request),
                input
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Query to get Management contracts
     *
     * @param request HTTP request
     * @param select the query to find Management contracts
     * @return Response
     */
    @POST
    @Path("/managementcontracts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("managementcontracts:read")
    public Response findManagementContracts(@Context HttpServletRequest request, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
                RequestResponse<ManagementContractModel> response = adminClient.findManagementContracts(
                    userInterfaceTransactionManager.getVitamContext(request),
                    query
                );
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
     * Query to Management contracts by id
     *
     * @param request HTTP request
     * @param id of the requested management contract
     * @return Response
     */
    @GET
    @Path("/managementcontracts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("managementcontracts:read")
    public Response findManagementContract(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<ManagementContractModel> response = adminClient.findManagementContractById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getHttpCode()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update Management contracts
     *
     * @param request HTTP request
     * @param contractId the id of management contract
     * @return Response
     */
    @POST
    @Path("/managementcontracts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("managementcontracts:update")
    public Response updateManagementContracts(
        @Context HttpServletRequest request,
        @PathParam("id") String contractId,
        JsonNode updateOptions
    ) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, contractId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(contractId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateOptions));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            Update updateRequest = new Update();
            if (!updateOptions.isObject()) {
                throw new InvalidCreateOperationException("Query not valid");
            }
            updateRequest.addActions(UpdateActionHelper.set((ObjectNode) updateOptions));
            final RequestResponse archiveDetails = adminClient.updateManagementContract(
                userInterfaceTransactionManager.getVitamContext(request),
                contractId,
                updateRequest.getFinalUpdateById()
            );
            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update context
     *
     * @param request HTTP request
     * @param contextId the id of context
     * @return Response
     */
    @POST
    @Path("/contexts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contexts:update")
    public Response updateContexts(
        @Context HttpServletRequest request,
        @PathParam("id") String contextId,
        JsonNode updateOptions
    ) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, contextId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(contextId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateOptions));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            Update updateRequest = new Update();
            if (!updateOptions.isObject()) {
                throw new InvalidCreateOperationException("Query not valid");
            }
            updateRequest.addActions(UpdateActionHelper.set((ObjectNode) updateOptions));
            final RequestResponse updateResponse = adminClient.updateContext(
                userInterfaceTransactionManager.getVitamContext(request),
                contextId,
                updateRequest.getFinalUpdateById()
            );
            LOGGER.error("update status " + updateResponse.toString());
            return Response.status(Status.OK).entity(updateResponse).build();
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * upload context
     *
     * @param request HTTP request
     * @param input the file json
     * @return Response
     */
    @POST
    @Path("/contexts")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contexts:create")
    public Response uploadContext(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createContexts(
                userInterfaceTransactionManager.getVitamContext(request),
                input
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get contexts
     *
     * @param request HTTP request
     * @param select
     * @return Response
     */
    @POST
    @Path("/contexts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contexts:read")
    public Response findContext(@Context HttpServletRequest request, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
                RequestResponse<ContextModel> response = adminClient.findContexts(
                    userInterfaceTransactionManager.getVitamContext(request),
                    query
                );
                if (response != null && response instanceof RequestResponseOK) {
                    return Response.status(Status.OK).entity(response).build();
                }
                if (response != null && response instanceof VitamError) {
                    LOGGER.error(response.toString());
                    return Response.status(response.getHttpCode()).entity(response).build();
                }
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get context by id
     *
     * @param request HTTP request
     * @param id
     * @return Response
     */
    @GET
    @Path("/contexts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contexts:read")
    public Response findContextByID(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<ContextModel> response = adminClient.findContextById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getHttpCode()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create profiles metadata
     *
     * @param request HTTP request
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/profiles")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:create")
    public Response createProfilesMetadata(@Context HttpServletRequest request, InputStream input) throws IOException {
        // want a creation
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createProfiles(
                userInterfaceTransactionManager.getVitamContext(request),
                input
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload profile xsd or rng
     *
     * @param request HTTP request
     * @param input the format file CSV
     * @return Response
     */
    @PUT
    @Path("/profiles/{id}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:create")
    public Response importProfileFile(
        @Context HttpServletRequest request,
        InputStream input,
        @PathParam("id") String id
    ) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createProfileFile(
                userInterfaceTransactionManager.getVitamContext(request),
                id,
                input
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update the detail of the profile
     *
     * @param request HTTP request
     * @param profileMetadataId
     * @param updateOptions
     * @return Response
     */
    @PUT
    @Path("/profiles/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:update")
    public Response updateProfile(
        @Context HttpServletRequest request,
        @PathParam("id") String profileMetadataId,
        JsonNode updateOptions
    ) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            Update updateRequest = new Update();
            if (!updateOptions.isObject()) {
                throw new InvalidCreateOperationException("Query not valid");
            }
            updateRequest.addActions(UpdateActionHelper.set((ObjectNode) updateOptions));
            RequestResponse response = adminClient.updateProfile(
                userInterfaceTransactionManager.getVitamContext(request),
                profileMetadataId,
                updateRequest.getFinalUpdateById()
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/profiles/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("profiles:read")
    public void downloadProfileFile(
        @Context HttpServletRequest request,
        @PathParam("id") String profileMetadataId,
        @Suspended final AsyncResponse asyncResponse
    ) {
        ParametersChecker.checkParameter("Profile id should be filled", profileMetadataId);
        Integer tenantId = userInterfaceTransactionManager.getTenantId(request);
        String contractId = userInterfaceTransactionManager.getContractId(request);
        String personalCert = userInterfaceTransactionManager.getPersonalCertificate(request);
        threadPoolExecutor.execute(
            () -> asyncDownloadProfileFile(profileMetadataId, asyncResponse, tenantId, contractId, personalCert)
        );
    }

    private void asyncDownloadProfileFile(
        String profileMetadataId,
        final AsyncResponse asyncResponse,
        Integer tenantId,
        String contractId,
        String personalCert
    ) {
        AsyncInputStreamHelper helper;

        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
            final Response response = client.downloadProfileFile(
                userInterfaceTransactionManager.getVitamContext(tenantId, contractId, personalCert),
                profileMetadataId
            );
            helper = new AsyncInputStreamHelper(asyncResponse, response);
            final Response.ResponseBuilder responseBuilder = Response.status(Status.OK)
                .header(CONTENT_DISPOSITION, response.getHeaderString(CONTENT_DISPOSITION))
                .type(response.getMediaType());
            helper.writeResponse(responseBuilder);
        } catch (final AccessExternalNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            asyncResponseResume(
                asyncResponse,
                Response.status(Status.NOT_FOUND)
                    .entity(getErrorEntity(Status.NOT_FOUND, exc.getMessage()).toString())
                    .build()
            );
        } catch (final AccessExternalClientException exc) {
            LOGGER.error(exc.getMessage(), exc);
            asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exc.getMessage()).toString())
                    .build()
            );
        }
    }

    private VitamError getErrorEntity(Status status, String msgErr) {
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setState(CODE_VITAM)
            .setMessage(status.getReasonPhrase())
            .setDescription(msgErr);
    }

    /**
     * Query to get profiles
     *
     * @param request HTTP request
     * @param select the query to find access contracts
     * @return Response
     */
    @POST
    @Path("/profiles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:read")
    public Response findProfiles(@Context HttpServletRequest request, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
                RequestResponse<ProfileModel> response = adminClient.findProfiles(
                    userInterfaceTransactionManager.getVitamContext(request),
                    query
                );
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
     * Query to Access contracts by id
     *
     * @param request HTTP request
     * @param id of the requested access contract
     * @return Response
     */
    @GET
    @Path("/profiles/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:read")
    public Response findProfileByID(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<ProfileModel> response = adminClient.findProfileById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getHttpCode()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private File downloadAndSaveATR(String guid, HttpServletRequest request) throws VitamClientException {
        File file = null;
        Response response = null;
        try (IngestExternalClient ingestExternalClient = ingestExternalClientFactory.getClient()) {
            response = ingestExternalClient.downloadObjectAsync(
                userInterfaceTransactionManager.getVitamContext(request),
                guid,
                IngestCollection.ARCHIVETRANSFERREPLY
            );
            InputStream inputStream = response.readEntity(InputStream.class);
            if (inputStream != null) {
                file = PropertiesUtils.fileFromTmpFolder("ATR_" + guid + ".xml");
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    StreamUtils.copy(inputStream, fileOutputStream);
                } catch (IOException e) {
                    throw new VitamClientException("Error during ATR generation");
                }
            }
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(response);
        }

        return file;
    }

    /**
     * Starts a TRACEABILITY check process
     *
     * @param request HTTP request
     * @param operationCriteria a DSLQuery to find the TRACEABILITY operation to verify
     * @return TRACEABILITY check process : the logbookOperation created during this process
     */
    @POST
    @Path("/traceability/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("traceability:check:create")
    public Response checkOperationTraceability(@Context HttpServletRequest request, String operationCriteria) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, operationCriteria);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(operationCriteria));

            // Prepare DSLQuery based on the received criteria
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(operationCriteria);
            final JsonNode dslQuery = dslQueryHelper.createSingleQueryDSL(optionsMap);

            // Start check process
            RequestResponse<JsonNode> result = userInterfaceTransactionManager.checkTraceabilityOperation(
                dslQuery,
                userInterfaceTransactionManager.getVitamContext(request)
            );

            // By default the returned status is different from the result of the verification process because we are
            // returning the report
            return Response.status(Status.OK).entity(result).build();
        } catch (AccessExternalClientServerException e) {
            LOGGER.error(Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(
                    new VitamError(status.name())
                        .setHttpCode(status.getStatusCode())
                        .setContext(ServiceName.VITAM.getName())
                        .setState(CODE_VITAM)
                        .setMessage(status.getReasonPhrase())
                        .setDescription(e.getMessage())
                )
                .build();
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(
                    new VitamError(status.name())
                        .setHttpCode(status.getStatusCode())
                        .setContext(ServiceName.VITAM.getName())
                        .setState(CODE_VITAM)
                        .setMessage(status.getReasonPhrase())
                        .setDescription(e.getMessage())
                )
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(e);
            final Status status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(
                    new VitamError(status.name())
                        .setHttpCode(status.getStatusCode())
                        .setContext(ServiceName.VITAM.getName())
                        .setState(CODE_VITAM)
                        .setMessage(status.getReasonPhrase())
                        .setDescription(e.getMessage())
                )
                .build();
        }
    }

    /**
     * Download the Traceability Operation file
     *
     * @param request HTTP request
     * @param operationId the TRACEABILITY operation identifier
     * @param contractId the contractId
     * @param tenantIdParam theTenantId
     * @param asyncResponse the async response
     */
    @GET
    @Path("/traceability/{idOperation}/content")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("traceability:content:read")
    public void downloadTraceabilityFile(
        @Context HttpServletRequest request,
        @PathParam("idOperation") String operationId,
        @QueryParam("contractId") String contractId,
        @QueryParam("tenantId") String tenantIdParam,
        @Suspended final AsyncResponse asyncResponse
    ) {
        // Check parameters
        ParametersChecker.checkParameter("Operation Id should be filled", operationId);
        Integer tenantId;
        // Get tenantId from headers
        if (tenantIdParam != null && StringUtils.isNumeric(tenantIdParam)) {
            tenantId = Integer.parseInt(tenantIdParam);
        } else {
            tenantId = userInterfaceTransactionManager.getTenantId(request);
        }
        String personalCert = userInterfaceTransactionManager.getPersonalCertificate(request);
        threadPoolExecutor.execute(
            () -> downloadTraceabilityFileAsync(asyncResponse, operationId, tenantId, contractId, personalCert)
        );
    }

    private void downloadTraceabilityFileAsync(
        final AsyncResponse asyncResponse,
        String operationId,
        Integer tenantId,
        String contractId,
        String personalCert
    ) {
        Response response = null;
        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
            response = client.downloadTraceabilityOperationFile(
                userInterfaceTransactionManager.getVitamContext(tenantId, contractId, personalCert),
                operationId
            );

            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);

            if (response.getStatus() == Status.OK.getStatusCode()) {
                helper.writeResponse(
                    Response.ok().header(CONTENT_DISPOSITION, response.getHeaderString(CONTENT_DISPOSITION))
                );
            } else {
                helper.writeResponse(Response.status(response.getStatus()));
            }
        } catch (IllegalArgumentException exc) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (AccessExternalClientServerException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build()
            );
        } catch (AccessUnauthorizedException e) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * Extract information from timestamp
     *
     * @param timestamp the timestamp to be transformed
     * @return Response
     */
    @POST
    @Path("/traceability/extractTimestamp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("logbook:operations:read")
    public Response extractTimeStampInformation(String timestamp) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, timestamp);
            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(timestamp);
            if (optionsMap.get("timestamp") != null) {
                JsonNode jsonNode = userInterfaceTransactionManager.extractInformationFromTimestamp(
                    optionsMap.get("timestamp")
                );
                return Response.status(Status.OK).entity(jsonNode).build();
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }
        } catch (BadRequestException | InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("/workflows")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkflowDefinitions(@Context HttpServletRequest request) {
        try (AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<WorkFlow> result = adminClient.getWorkflowDefinitions(
                userInterfaceTransactionManager.getVitamContext(request)
            );
            return Response.status(Status.OK).entity(result).build();
        } catch (VitamClientException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/audits")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:audit")
    public Response launchAudit(@Context HttpServletRequest request, JsonNode auditOption) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<JsonNode> result = adminClient.launchAudit(
                userInterfaceTransactionManager.getVitamContext(request),
                auditOption
            );
            return Response.status(Status.OK).entity(result).build();
        } catch (Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload Service Agencies
     *
     * @param request HTTP request
     * @param input the Service Agency file CSV
     * @return Response
     */
    @POST
    @Path("/agencies")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("agencies:create")
    public Response uploadServiceAgencies(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createAgencies(
                userInterfaceTransactionManager.getVitamContext(request),
                input,
                request.getHeader(GlobalDataRest.X_FILENAME)
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    /**
     * Find Service Agencies by DSL
     *
     * @param request HTTP request
     * @param select the query to find Service Agency
     * @return Response
     */
    @POST
    @Path("/agencies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("agencies:read")
    public Response findServiceAgencies(@Context HttpServletRequest request, String select) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, select);
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
                RequestResponse<AgenciesModel> response = adminClient.findAgencies(
                    userInterfaceTransactionManager.getVitamContext(request),
                    query
                );
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
     * Query to Service Agency by identifier
     *
     * @param request HTTP request
     * @param id of the requested Service Agency
     * @return Response
     */
    @GET
    @Path("/agencies/{id:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("agencies:read")
    public Response findServiceAgencyById(@Context HttpServletRequest request, @PathParam("id") String id) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, id);

        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<AgenciesModel> response = adminClient.findAgencyByID(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getHttpCode()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create archive unit profiles metadata
     *
     * @param request HTTP request
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/archiveunitprofiles")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveunitprofiles:create")
    public Response createArchiveUnitProfileMetadata(@Context HttpServletRequest request, InputStream input)
        throws IOException {
        // want a creation
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.createArchiveUnitProfile(
                userInterfaceTransactionManager.getVitamContext(request),
                input
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update the detail of the archive unit profile
     *
     * @param request HTTP request
     * @param archiveUnitprofileId
     * @param updateOptions
     * @return Response
     */
    @PUT
    @Path("/archiveunitprofiles/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveunitprofiles:update")
    public Response updateArchiveUnitProfile(
        @Context HttpServletRequest request,
        @PathParam("id") String archiveUnitprofileId,
        JsonNode updateOptions
    ) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            Update updateRequest = new Update();
            if (!updateOptions.isObject()) {
                throw new InvalidCreateOperationException("Query not valid");
            }
            updateRequest.addActions(UpdateActionHelper.set((ObjectNode) updateOptions));
            RequestResponse response = adminClient.updateArchiveUnitProfile(
                userInterfaceTransactionManager.getVitamContext(request),
                archiveUnitprofileId,
                updateRequest.getFinalUpdateById()
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getStatus()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Query to get archive unit profiles
     *
     * @param request HTTP request
     * @param select the query to find archive unit profiles
     * @return Response
     */
    @POST
    @Path("/archiveunitprofiles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveunitprofiles:read")
    public Response findArchiveUnitProfiles(@Context HttpServletRequest request, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
                RequestResponse<ArchiveUnitProfileModel> response = adminClient.findArchiveUnitProfiles(
                    userInterfaceTransactionManager.getVitamContext(request),
                    query
                );
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
     * Query to Access archive unit profile by id
     *
     * @param request HTTP request
     * @param id of the requested archive unit profile
     * @return Response
     */
    @GET
    @Path("/archiveunitprofiles/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveunitprofiles:read")
    public Response findArchiveUnitProfileByID(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<ArchiveUnitProfileModel> response = adminClient.findArchiveUnitProfileById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getHttpCode()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Import the ontologies json file
     *
     * @param request HTTP request
     * @param input a json file
     * @return Response
     */
    @POST
    @Path("/ontologies")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("ontologies:create")
    public Response importOntologies(
        @HeaderParam(GlobalDataRest.FORCE_UPDATE) boolean forceUpdate,
        @Context HttpServletRequest request,
        InputStream input
    ) throws IOException {
        // want a creation
        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse response = adminClient.importOntologies(
                forceUpdate,
                userInterfaceTransactionManager.getVitamContext(request),
                input
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Query to find all ontologies matching given criteria
     *
     * @param request HTTP request context
     * @param select given criteria in order to select ontologies
     * @return Response
     */
    @POST
    @Path("/ontologies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("ontologies:read")
    public Response findOntologies(@Context HttpServletRequest request, String select) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, select);
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
                RequestResponse<OntologyModel> response = adminClient.findOntologies(
                    userInterfaceTransactionManager.getVitamContext(request),
                    query
                );
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
     * Query to get Ontology by identifier
     *
     * @param request HTTP request
     * @param id of the requested Service Agency
     * @return Response
     */
    @GET
    @Path("/ontologies/{id:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("ontologies:read")
    public Response findOntologyById(@Context HttpServletRequest request, @PathParam("id") String id) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, id);

        try (final AdminExternalClient adminClient = adminExternalClientFactory.getClient()) {
            RequestResponse<OntologyModel> response = adminClient.findOntologyById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response != null && response instanceof RequestResponseOK) {
                return Response.status(Status.OK).entity(response).build();
            }
            if (response != null && response instanceof VitamError) {
                LOGGER.error(response.toString());
                return Response.status(response.getHttpCode()).entity(response).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Send a queryDSL request in order to select some units and create a matching DIP
     *
     * @param request HTTP request
     * @param criteria queryDSL for criteria
     */
    @POST
    @Path("/archiveunit/evidenceaudit")
    @RequiresPermissions("evidenceaudit:check")
    public Response launchEvidanceAudit(@Context HttpServletRequest request, String criteria) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        try {
            JsonNode queryDSL = JsonHandler.getFromString(criteria);
            final RequestResponse response = userInterfaceTransactionManager.evidenceAudit(
                queryDSL,
                userInterfaceTransactionManager.getVitamContext(request)
            );
            return Response.status(Status.OK).entity(response).build();
        } catch (VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * -
     * Send a queryDSL request in order to generate an probative value
     *
     * @param request HTTP request
     * @param criteria queryDSL for criteria
     */
    @POST
    @Path("/archiveunit/probativevalueexport")
    @RequiresPermissions("probativevalue:check")
    public Response exportProbativeValue(@Context HttpServletRequest request, String criteria) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        try {
            ProbativeValueRequest queryDSL = JsonHandler.getFromString(criteria, ProbativeValueRequest.class);
            RequestResponse response = userInterfaceTransactionManager.exportProbativeValue(
                queryDSL,
                userInterfaceTransactionManager.getVitamContext(request)
            );
            return Response.status(Status.OK).entity(response).build();
        } catch (VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * Send a queryDSL request in order to select some units and create a matching DIP
     *
     * @param request HTTP request
     * @param criteria queryDSL for criteria
     */
    @POST
    @Path("/archiveunit/dipexport")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("dipexportv2:create")
    public Response createDIPForExport(@Context HttpServletRequest request, DipRequest criteria) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        try {
            final RequestResponse<JsonNode> response = userInterfaceTransactionManager.exportDIP(
                criteria,
                userInterfaceTransactionManager.getVitamContext(request)
            );
            return Response.status(Status.OK).entity(response).build();
        } catch (VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/archiveunit/transfers")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("transfers:create")
    public Response createTransferSIP(@Context HttpServletRequest request, TransferRequest criteria) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        try {
            final RequestResponse<JsonNode> response = userInterfaceTransactionManager.transferSIP(
                criteria,
                userInterfaceTransactionManager.getVitamContext(request)
            );
            return Response.status(Status.OK).entity(response).build();
        } catch (VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Send a DIP id request in order to download the matching DIP
     *
     * @param request HTTP request
     * @param asyncResponse request asynchronized response
     */
    @GET
    @Path("/archiveunit/dipexport/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("dipexport:read")
    public void getDIPAsInputStreamAsync(
        @Context HttpServletRequest request,
        @PathParam("id") String id,
        @Suspended final AsyncResponse asyncResponse
    ) {
        Integer tenantId = userInterfaceTransactionManager.getTenantId(request);
        String contractId = userInterfaceTransactionManager.getContractId(request);
        String personalCert = userInterfaceTransactionManager.getPersonalCertificate(request);
        threadPoolExecutor.execute(() -> {
            asyncGetExtportStream(asyncResponse, id, tenantId, contractId, personalCert, false);
        });
    }

    @GET
    @Path("/archiveunit/transfers/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("transfers:read")
    public void getTransferSIPAsInputStreamAsync(
        @Context HttpServletRequest request,
        @PathParam("id") String id,
        @Suspended final AsyncResponse asyncResponse
    ) {
        Integer tenantId = userInterfaceTransactionManager.getTenantId(request);
        String contractId = userInterfaceTransactionManager.getContractId(request);
        String personalCert = userInterfaceTransactionManager.getPersonalCertificate(request);
        threadPoolExecutor.execute(() -> {
            asyncGetExtportStream(asyncResponse, id, tenantId, contractId, personalCert, true);
        });
    }

    private void asyncGetExtportStream(
        AsyncResponse asyncResponse,
        String dipId,
        Integer tenantId,
        String contracId,
        String personalCert,
        boolean isTransfer
    ) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(dipId));
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, dipId);
        } catch (final InvalidParseOperationException exc) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
            return;
        } catch (final IllegalArgumentException exc) {
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.PRECONDITION_FAILED).build()
            );
            return;
        }
        try {
            userInterfaceTransactionManager.downloadExports(
                asyncResponse,
                dipId,
                userInterfaceTransactionManager.getVitamContext(tenantId, contracId, personalCert),
                isTransfer
            );
        } catch (final VitamClientException exc) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build()
            );
        } catch (final Exception exc) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build()
            );
        }
    }

    /**
     * Compute inherated rules from basket
     *
     * @param request HTTP request
     * @param Query contains updated field
     * @return archive unit details
     */
    @POST
    @Path("/archiveunit/computedinheritedrules")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveupdate:units:update")
    public Response computedInheritedRules(@Context HttpServletRequest request, String Query) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(Query));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        try {
            final JsonNode idFields = JsonHandler.getFromString(Query);
            RequestResponse<JsonNode> metadataUpdateResponse = userInterfaceTransactionManager.computinheritedrules(
                idFields,
                userInterfaceTransactionManager.getVitamContext(request)
            );

            if (metadataUpdateResponse == null) {
                return Response.status(Status.BAD_REQUEST).build();
            }

            return metadataUpdateResponse.toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete Compute inherated rules from basket
     *
     * @param request HTTP request
     * @param Query contains updated field
     * @return archive unit details
     */
    @POST
    @Path("/archiveunit/deletecomputedinheritedrules")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveupdate:units:update")
    public Response deleteComputedInheritedRules(@Context HttpServletRequest request, String Query) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(Query));
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        try {
            final JsonNode idFields = JsonHandler.getFromString(Query);
            RequestResponse<JsonNode> metadataUpdateResponse =
                userInterfaceTransactionManager.deleteComputedInheritedRules(
                    idFields,
                    userInterfaceTransactionManager.getVitamContext(request)
                );

            if (metadataUpdateResponse == null) {
                return Response.status(Status.BAD_REQUEST).build();
            }

            return metadataUpdateResponse.toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
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
     * Return admin tenant
     *
     * @return admin tenant number
     */
    @GET
    @Path("/admintenant")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAdminTenant() {
        return Response.status(Status.OK).entity(VitamConfiguration.getAdminTenant()).build();
    }

    /**
     * @param request needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param criteria criteria search for units
     * @return Reponse
     */
    @POST
    @Path("/objectssearch/objects")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("objects:read")
    public Response getObjectsSearchResult(
        @Context HttpServletRequest request,
        @CookieParam("JSESSIONID") String sessionId,
        String criteria
    ) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        String requestId;
        RequestResponse result;
        RequestResponse resultUnits;
        OffsetBasedPagination pagination;
        try {
            Enumeration<String> headersReqId = request.getHeaders(IhmWebAppHeader.REQUEST_ID.name());
            while (headersReqId.hasMoreElements()) {
                SanityChecker.checkParameter(headersReqId.nextElement());
            }
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
                result = RequestResponseOK.getFromJsonNode(paginationHelper.getResult(sessionId, pagination));

                if (!result.isOk()) {
                    return result.toResponse();
                }

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
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(criteria));

                final Map<String, Object> criteriaMap = JsonHandler.getMapFromString(criteria);
                final JsonNode preparedQueryDsl = dslQueryHelper.createSelectElasticsearchDSLQuery(criteriaMap);

                result = userInterfaceTransactionManager.searchObjects(
                    preparedQueryDsl,
                    userInterfaceTransactionManager.getVitamContext(request)
                );

                if (!result.isOk()) {
                    return result.toResponse();
                }

                ArrayNode results = (ArrayNode) result.toJsonNode().get("$results");
                if (results != null && results.size() > 0) {
                    StringBuilder unitsTitleCriteria = new StringBuilder();
                    unitsTitleCriteria.append("{\"$roots\": [],\"$query\": [{\"$in\": {\"#object\": [");
                    for (JsonNode res : results) {
                        unitsTitleCriteria.append(res.get("#id").toString()).append(",");
                    }
                    unitsTitleCriteria.deleteCharAt(unitsTitleCriteria.length() - 1);
                    unitsTitleCriteria.append(
                        "]}}],\"$filter\": {\"$limit\": 10000},\"$projection\": {\"$fields\": {\"Title\": 1,\"#object\": 1}},\"$facets\": []}"
                    );
                    ObjectMapper mapper = new ObjectMapper();
                    final JsonNode preparedQueryDslUnits = mapper.readTree(unitsTitleCriteria.toString());
                    resultUnits = userInterfaceTransactionManager.searchUnits(
                        preparedQueryDslUnits,
                        userInterfaceTransactionManager.getVitamContext(request)
                    );
                    if (resultUnits.isOk()) {
                        ArrayNode resultsUnits = (ArrayNode) resultUnits.toJsonNode().get("$results");
                        for (JsonNode resU : resultsUnits) {
                            String currentObjectId = resU.get("#object").toString();
                            for (JsonNode res : results) {
                                if (currentObjectId.equals(res.get("#id").toString())) {
                                    if (res.get("UnitsTitle") != null) {
                                        ((ArrayNode) res.get("UnitsTitle")).add(resU.get("Title"));
                                    } else {
                                        ((ObjectNode) res).putArray("UnitsTitle").add(resU.get("Title"));
                                    }
                                }
                            }
                        }
                    }
                }
                // save result
                ObjectNode finalResult = (ObjectNode) result.toJsonNode();
                finalResult.putArray("$results").addAll(results);
                paginationHelper.setResult(sessionId, (JsonNode) finalResult);
                // pagination
                result = RequestResponseOK.getFromJsonNode(
                    paginationHelper.getResult((JsonNode) finalResult, pagination)
                );

                return Response.status(Status.OK).entity(result).build();
            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
                return Response.status(Status.BAD_REQUEST).build();
            } catch (final AccessExternalClientNotFoundException e) {
                LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
                return Response.status(Status.NOT_FOUND).build();
            } catch (final AccessUnauthorizedException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                return Response.status(Status.UNAUTHORIZED).build();
            } catch (final VitamClientException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}
