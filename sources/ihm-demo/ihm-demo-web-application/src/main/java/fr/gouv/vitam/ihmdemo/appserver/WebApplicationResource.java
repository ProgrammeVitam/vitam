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
package fr.gouv.vitam.ihmdemo.appserver;


import static fr.gouv.vitam.common.server.application.AsyncInputStreamHelper.asyncResponseResume;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Iterables;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.api.ErrorMessage;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalNotFoundException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamStreamingOutput;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import fr.gouv.vitam.ihmdemo.common.pagination.OffsetBasedPagination;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.common.utils.PermissionReader;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Web Application Resource class
 */
@Path("/v1/api")
public class WebApplicationResource extends ApplicationStatusResource {

    public static final String X_SIZE_TOTAL = "X-Size-Total";
    public static final String X_CHUNK_OFFSET = "X-Chunk-Offset";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResource.class);

    private static final String CODE_VITAM = "code_vitam";
    private static final String BAD_REQUEST_EXCEPTION_MSG = "Bad request Exception";
    private static final String ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Access client unavailable";
    private static final String ACCESS_SERVER_EXCEPTION_MSG = "Access Server exception";
    private static final String INTERNAL_SERVER_ERROR_MSG = "INTERNAL SERVER ERROR";
    private static final String SEARCH_CRITERIA_MANDATORY_MSG = "Search criteria payload is mandatory";
    private static final String FIELD_ID_KEY = "fieldId";
    private static final String NEW_FIELD_VALUE_KEY = "newFieldValue";
    private static final String INVALID_ALL_PARENTS_TYPE_ERROR_MSG = "The parameter \"allParents\" is not an array";
    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";

    private static final String LOGBOOK_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Logbook Client NOT FOUND Exception";
    private static final ConcurrentMap<String, List<Object>> uploadRequestsStatus = new ConcurrentHashMap<>();
    private static final int COMPLETE_RESPONSE_SIZE = 3;
    private static final int GUID_INDEX = 0;
    private static final int ATR_CONTENT_INDEX = 2;
    private static final String STATUS_FIELD_QUERY = "Status";
    private static final String ARCHIVEPROFILES_FIELD_QUERY = "ArchiveProfiles";
    private static final String ACTIVATION_DATE_FIELD_QUERY = "ActivationDate";
    private static final String DEACTIVATION_DATE_FIELD_QUERY = "DeactivationDate";
    private static final String LAST_UPDATE_FIELD_QUERY = "LastUpdate";
    private static final String NAME_FIELD_QUERY = "Name";
    private static final String EVERY_ORIG_AGENCY_FIELD_QUERY = "EveryOriginatingAgency";

    private final WebApplicationConfig webApplicationConfig;
    private Map<String, AtomicLong> uploadMap = new HashMap<>();

    private final Set<String> permissions;

    /**
     * Constructor
     *
     * @param webApplicationConfig the web server ihm-demo configuration
     * @param permissions list of permissions
     */
    public WebApplicationResource(WebApplicationConfig webApplicationConfig, Set<String> permissions) {
        super(new BasicVitamStatusServiceImpl(), webApplicationConfig.getTenants());
        this.webApplicationConfig = webApplicationConfig;
        this.permissions = permissions;
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
     * @param headers needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param criteria criteria search for units
     * @return Reponse
     */
    @POST
    @Path("/archivesearch/units")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archivesearch:units:read")
    public Response getArchiveSearchResult(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
        String criteria) {

        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        String requestId;
        RequestResponse result;
        OffsetBasedPagination pagination = null;

        try {
            pagination = new OffsetBasedPagination(headers);
        } catch (final VitamException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        final List<String> requestIds = HttpHeaderHelper.getHeaderValues(headers, IhmWebAppHeader.REQUEST_ID.name());
        Integer tenantId = getTenantId(headers);
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
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(criteria));

                final Map<String, Object> criteriaMap = JsonHandler.getMapFromString(criteria);
                final JsonNode preparedQueryDsl = DslQueryHelper.createSelectElasticsearchDSLQuery(criteriaMap);
                result = UserInterfaceTransactionManager.searchUnits(preparedQueryDsl,
                    getTenantId(headers));

                // save result
                PaginationHelper.setResult(sessionId, result.toJsonNode());
                // pagination
                result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(result.toJsonNode(), pagination));

                return Response.status(Status.OK).entity(result).build();
            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
                return Response.status(Status.BAD_REQUEST).build();
            } catch (final AccessExternalClientServerException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (final AccessExternalClientNotFoundException e) {
                LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
                return Response.status(Status.NOT_FOUND).build();
            } catch (final AccessUnauthorizedException e) {
                LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
                return Response.status(Status.UNAUTHORIZED).build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    /**
     * @param headers needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param unitId archive unit id
     * @return archive unit details
     */
    @GET
    @Path("/archivesearch/unit/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archivesearch:units:read")
    public Response getArchiveUnitDetails(@Context HttpHeaders headers, @PathParam("id") String unitId) {

        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
            // Prepare required map
            final Map<String, String> selectUnitIdMap = new HashMap<>();
            selectUnitIdMap.put(UiConstants.SELECT_BY_ID.toString(), unitId);
            selectUnitIdMap.put(DslQueryHelper.PROJECTION_DSL, BuilderToken.GLOBAL.RULES.exactToken());

            final JsonNode preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(selectUnitIdMap);
            final RequestResponse archiveDetails = UserInterfaceTransactionManager
                .getArchiveUnitDetails(preparedQueryDsl, unitId, getTenantId(headers));

            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param headers header needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/logbook/operations")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("logbook:operations:read")
    public Response getLogbookResult(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
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
        Integer tenantId = getTenantId(headers);
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
                ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
                final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);

                result = UserInterfaceTransactionManager.selectOperation(query, tenantId);

                // save result
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
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .build();
            }
            return Response.status(Status.OK).entity(result).header(GlobalDataRest.X_REQUEST_ID, requestId)
                .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                .header(IhmDataRest.X_LIMIT, pagination.getLimit()).build();
        }
    }

    /**
     * @param headers needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param operationId id of operation
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/logbook/operations/{idOperation}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("logbook:operations:read")
    public Response getLogbookResultById(@Context HttpHeaders headers, @PathParam("idOperation") String operationId,
        String options) {
        String contractName = headers.getHeaderString(GlobalDataRest.X_ACCESS_CONTRAT_ID);
        RequestResponse result = null;
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result =
                UserInterfaceTransactionManager.selectOperationbyId(operationId, getTenantId(headers), contractName);
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
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * upload : API Endpoint that can Handle chunk mode. Chunks information are given in header (Fast catch of these
     * header are present in the code) <br />
     * The front should give some information
     * <ul>
     * <li>Flow-Chunk-Number => The index of the current chunk</li>
     * <li>Flow-Chunk-Size => The configured maximal size of a chunk</li>
     * <li>Flow-Current-Chunk-Size => The size of the current chunk</li>
     * <li>Flow-Total-Size => The total size of the file (All chunks)</li>
     * <li>Flow-Identifier => The identifier of the flow</li>
     * <li>Flow-Filename => The file name</li>
     * <li>Flow-Relative-Path => (?)The relative path (or the file name only)</li>
     * <li>Flow-Total-Chunks => The number of chunks</li>
     * </ul>
     *
     * @param request the http servlet request
     * @param response the http servlet response
     * @param stream data input stream for the current chunk
     * @param headers HTTP Headers containing chunk information
     * @return Response
     */
    @Path("ingest/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("ingest:create")
    public Response upload(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @Context HttpHeaders headers, byte[] stream) {
        String operationGuid = null;
        String chunkOffset = headers.getHeaderString(X_CHUNK_OFFSET);
        String chunkSizeTotal = headers.getHeaderString(X_SIZE_TOTAL);
        String contextId = headers.getHeaderString(GlobalDataRest.X_CONTEXT_ID);
        String action = headers.getHeaderString(GlobalDataRest.X_ACTION);

        if (headers.getHeaderString(GlobalDataRest.X_REQUEST_ID) == null ||
            headers.getHeaderString(GlobalDataRest.X_REQUEST_ID).isEmpty()) {
            // GUID operation (Server Application level)
            operationGuid = GUIDFactory.newGUID().getId();
            AtomicLong writtenByteSize = new AtomicLong(0);
            uploadMap.put(operationGuid, writtenByteSize);
        } else {
            operationGuid = headers.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        }

        RandomAccessFile randomAccessFile = null;
        FileChannel fileChannel = null;
        try {
            randomAccessFile = new RandomAccessFile(
                PropertiesUtils.fileFromTmpFolder(operationGuid).getAbsolutePath(), "rw");
            fileChannel = randomAccessFile.getChannel();
            long offset = Long.parseLong(chunkOffset);
            int writtenByte = fileChannel.write(ByteBuffer.wrap(stream), offset);
            AtomicLong writtenByteSize = uploadMap.get(operationGuid);
            long total = writtenByteSize.addAndGet(writtenByte);
            long size = Long.parseLong(chunkSizeTotal);
            if (total >= size) {
                fileChannel.force(false);
                startUpload(operationGuid, getTenantId(headers), contextId, action);
                uploadMap.remove(operationGuid);
            }
            return Response
                .status(Status.OK).entity(JsonHandler.getFromString(
                    "{\"" + GlobalDataRest.X_REQUEST_ID.toLowerCase() + "\":\"" + operationGuid + "\"}"))
                .header(GlobalDataRest.X_REQUEST_ID, operationGuid)
                .build();
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("Upload failed", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Upload failed", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .header(GlobalDataRest.X_REQUEST_ID, operationGuid).build();
        } finally {
            try {
                if (fileChannel != null) {
                    fileChannel.close();
                }
            } catch (IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            try {
                randomAccessFile.close();
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


            try (IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
                final RequestResponse<JsonNode> finalResponse =
                    client.upload(new FileInputStream(temporarSipFile), tenantId, contextId, action);

                int responseStatus = finalResponse.getHttpCode();
                final String guid = finalResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);

                final List<Object> finalResponseDetails = new ArrayList<>();
                finalResponseDetails.add(guid);
                finalResponseDetails.add(Status.fromStatusCode(responseStatus));
                uploadRequestsStatus.put(operationGuidFirstLevel, finalResponseDetails);

            } catch (IOException | VitamException e) {
                LOGGER.error("Upload failed", e);
                final List<Object> finalResponseDetails = new ArrayList<>();
                finalResponseDetails.add(operationGuidFirstLevel);
                finalResponseDetails.add(Status.INTERNAL_SERVER_ERROR);
                uploadRequestsStatus.put(operationGuidFirstLevel, finalResponseDetails);
            } finally {
                temporarSipFile.delete();
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
    public Response checkUploadOperation(@PathParam("id_op") String operationId, @Context HttpHeaders headers,
        @QueryParam("action") String action)
        throws VitamClientException, IngestExternalException {
        // TODO Need a tenantId test for checking upload (Only IHM-DEMO scope,
        // dont call VITAM backend) ?
        // 1- Check if the requested operation is done
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        // mapping X-request-ID
        final List<Object> responseDetails = uploadRequestsStatus.get(operationId);
        Integer tenantId = getTenantId(headers);
        String contractName = headers.getHeaderString(GlobalDataRest.X_ACCESS_CONTRAT_ID);

        if (responseDetails != null) {
            try (IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
                String id = responseDetails.get(GUID_INDEX).toString();

                if (client.wait(tenantId, id, 30, 1000l, TimeUnit.MILLISECONDS)) {

                    final ItemStatus itemStatus =
                        client.getOperationProcessExecutionDetails(id, JsonHandler.createObjectNode(), tenantId);
                    if (ProcessState.COMPLETED.equals(itemStatus.getGlobalState())) {
                        File file = downloadAndSaveATR(id, tenantId);

                        if (file != null) {
                            JsonNode lastEvent = getlogBookOperationStatus(id, tenantId, contractName);
                            // ingestExternalClient client
                            int status = getStatus(lastEvent);
                            return Response.status(status).entity(new FileInputStream(file))
                                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                                .header("Content-Disposition",
                                    "attachment; filename=ATR_" + id + ".xml")
                                .header(GlobalDataRest.X_REQUEST_ID, operationId).build();
                        }
                    } else if (ProcessState.PAUSE.equals(itemStatus.getGlobalState())) {
                        return Response.status(itemStatus.getGlobalStatus().getEquivalentHttpStatus()).build();
                    } else {
                        return Response.status(Status.NO_CONTENT).header(GlobalDataRest.X_REQUEST_ID, operationId)
                            .build();
                    }
                } else {
                    return Response.status(Status.NO_CONTENT).header(GlobalDataRest.X_REQUEST_ID, operationId).build();
                }

            } catch (Exception e) {
                LOGGER.error(e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).header(GlobalDataRest.X_REQUEST_ID, operationId)
                    .entity(e).build();
            }
        }
        // 2- Return the created GUID
        return Response.status(Status.NO_CONTENT).header(GlobalDataRest.X_REQUEST_ID, operationId)
            .build();

    }

    /**
     * Once done, clear the Upload operation history
     *
     * @param operationId the operation id
     * @return the Response
     */
    @Path("clear/{id_op}")
    @GET
    @RequiresPermissions("clear:delete")
    public Response clearUploadOperationHistory(@PathParam("id_op") String operationId) {
        // TODO Need a tenantId test for checking upload (Only IHM-DEMO scope,
        // dont call VITAM backend) ?
        final List<Object> responseDetails = uploadRequestsStatus.get(operationId);
        if (responseDetails != null) {
            // Clean up uploadRequestsStatus
            uploadRequestsStatus.remove(operationId);
            if (responseDetails.size() == COMPLETE_RESPONSE_SIZE) {
                final File file = (File) responseDetails.get(ATR_CONTENT_INDEX);
                file.delete();
            }
            File file = PropertiesUtils.fileFromTmpFolder("ATR_" + operationId + ".xml");
            if (file != null) {
                file.delete();
            }
            // Cleaning process succeeded
            return Response.status(Status.OK).header(GlobalDataRest.X_REQUEST_ID, operationId).build();
        } else {
            // Cleaning process failed
            return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, operationId).build();
        }
    }

    private static JsonNode getlogBookOperationStatus(String operationId, Integer tenantId, String contractName)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        final RequestResponse<JsonNode> result =
            UserInterfaceTransactionManager.selectOperationbyId(operationId, tenantId, contractName);
        RequestResponseOK<JsonNode> responseOK = (RequestResponseOK<JsonNode>) result;
        List<JsonNode> results = responseOK.getResults();
        JsonNode operation = results.get(0);

        ArrayNode events = (ArrayNode) operation.get("events");
        JsonNode lastEvent = Iterables.getLast(events);
        return lastEvent;
    }

    private static int getStatus(JsonNode lastEvent) throws Exception {
        String out = String.valueOf(lastEvent.get("outcome").asText());
        if (out == null) {
            throw new Exception("parsing Error");
        }
        switch (out) {
            case "WARNING":
                return 206;
            case "OK":
                return 200;
            case "KO":
                return 400;

        }
        return 500;
    }

    /**
     * Update Archive Units
     *
     * @param headers HTTP Headers
     * @param updateSet contains updated field
     * @param unitId archive unit id
     * @return archive unit details
     */
    @POST
    @Path("/archiveupdate/units/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveupdate:units:update")
    public Response updateArchiveUnitDetails(@Context HttpHeaders headers, @PathParam("id") String unitId,
        String updateSet) {
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
            final Map<String, String> updateUnitIdMap = new HashMap<>();
            final JsonNode modifiedFields = JsonHandler.getFromString(updateSet);
            if (modifiedFields != null && modifiedFields.isArray()) {
                for (final JsonNode modifiedField : modifiedFields) {
                    updateUnitIdMap.put(modifiedField.get(FIELD_ID_KEY).textValue(),
                        modifiedField.get(NEW_FIELD_VALUE_KEY).textValue());
                }
            }

            // Add ID to set root part
            updateUnitIdMap.put(UiConstants.SELECT_BY_ID.toString(), unitId);
            final JsonNode preparedQueryDsl = DslQueryHelper.createUpdateDSLQuery(updateUnitIdMap);
            final RequestResponse archiveDetails =
                UserInterfaceTransactionManager.updateUnits(preparedQueryDsl, unitId,
                    getTenantId(headers));
            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final NoWritingPermissionException e) {
            return Response.status(Status.METHOD_NOT_ALLOWED).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    // /**
    // * @param headers HTTP header needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
    // * @param sessionId json session id from shiro
    // * @param options the queries for searching
    // * @return Response
    // */
    // @POST
    // @Path("/admin/formats")
    // @Produces(MediaType.APPLICATION_JSON)
    // @RequiresPermissions("admin:formats:read")
    // public Response getFileFormats(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
    // String options) {
    //
    // ParametersChecker.checkParameter("cookie is mandatory", sessionId);
    // String requestId = null;
    // RequestResponse result = null;
    // OffsetBasedPagination pagination = null;
    //
    // try {
    // pagination = new OffsetBasedPagination(headers);
    // } catch (final VitamException e) {
    // LOGGER.error("Bad request Exception ", e);
    // return Response.status(Status.BAD_REQUEST).build();
    // }
    // final List<String> requestIds = HttpHeaderHelper.getHeaderValues(headers, IhmWebAppHeader.REQUEST_ID.name());
    // Integer tenantId = getTenantId(headers);
    // if (requestIds != null) {
    // requestId = requestIds.get(0);
    // // get result from shiro session
    // try {
    // result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(sessionId, pagination));
    //
    // // save result
    // PaginationHelper.setResult(sessionId, result.toJsonNode());
    // // pagination
    // result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(result.toJsonNode(), pagination));
    //
    // return Response.status(Status.OK).entity(result).header(GlobalDataRest.X_REQUEST_ID, requestId)
    // .header(IhmDataRest.X_OFFSET, pagination.getOffset())
    // .header(IhmDataRest.X_LIMIT, pagination.getLimit()).build();
    // } catch (final VitamException e) {
    // LOGGER.error("Bad request Exception ", e);
    // return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
    // }
    // } else {
    // requestId = GUIDFactory.newRequestIdGUID(tenantId).toString();
    //
    // ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
    // try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
    // SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
    // final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
    // final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);
    // result = adminClient.findDocuments(AdminCollections.FORMATS, query,
    // getTenantId(headers));
    //
    // // save result
    // PaginationHelper.setResult(sessionId, result.toJsonNode());
    // // pagination
    // result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(result.toJsonNode(), pagination));
    //
    // return Response.status(Status.OK).entity(result).build();
    // } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
    // LOGGER.error("Bad request Exception ", e);
    // return Response.status(Status.BAD_REQUEST).build();
    // } catch (final AccessExternalClientNotFoundException e) {
    // LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
    // return Response.status(Status.NOT_FOUND).build();
    // } catch (final Exception e) {
    // LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
    // return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    // }
    // }
    // }


    /**
     * @param headers HTTP header needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/formats")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:formats:read")
    public Response getFileFormats(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
        String options) {
        // FIXME P0: Pagination rollbacked because of error on mongo/ES indexation --> use the commented method after
        // some fixes
        // FIXME Pagination should be use as in others endpoints after solution found (See Item #2227)
        ParametersChecker
            .checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
        try (final AdminExternalClient adminClient = AdminExternalClientFactory
            .getInstance().getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            final RequestResponse result = adminClient.findDocuments(AdminCollections.FORMATS, query,
                getTenantId(headers));
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param headers HTTP Headers
     * @param formatId id of format
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/formats/{idFormat}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:formats:read")
    public Response getFormatById(@Context HttpHeaders headers, @PathParam("idFormat") String formatId,
        String options) {
        RequestResponse result = null;

        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            ParametersChecker.checkParameter("Format Id is mandatory", formatId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
            result = adminClient.findDocumentById(AdminCollections.FORMATS, formatId, getTenantId(headers));
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
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
     * @param headers HTTP Headers
     * @param input the format file xml
     * @return If the formet is valid, return ok. If not, return the list of errors
     */
    @POST
    @Path("/format/check")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("format:check")
    public Response checkRefFormat(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            Status status = adminClient.checkDocuments(AdminCollections.FORMATS, input, getTenantId(headers));
            return Response.status(status).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
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
     * @param headers HTTP Headers
     * @param input the format file xml
     * @return Response
     */
    @POST
    @Path("/format/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("format:create")
    public Response uploadRefFormat(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            Status status = adminClient.createDocuments(AdminCollections.FORMATS, input, getTenantId(headers));
            return Response.status(status).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.FORBIDDEN).build();
        } catch (final Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    /**
     * Retrieve an ObjectGroup as Json data based on the provided ObjectGroup id
     *
     * @param headers HTTP Headers
     * @param objectGroupId the object group Id
     * @return a response containing a json with informations about usages and versions for an object group
     */
    @GET
    @Path("/archiveunit/objects/{idOG}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveunit:objects:read")
    public Response getArchiveObjectGroup(@Context HttpHeaders headers, @PathParam("idOG") String objectGroupId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));

            final HashMap<String, String> qualifierProjection = new HashMap<>();
            qualifierProjection.put("projection_qualifiers", "#qualifiers");
            final JsonNode preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(qualifierProjection);
            final RequestResponse searchResult = UserInterfaceTransactionManager.selectObjectbyId(preparedQueryDsl,
                objectGroupId, getTenantId(headers));

            return Response.status(Status.OK).entity(JsonTransformer.transformResultObjects(searchResult.toJsonNode()))
                .build();

        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Retrieve an Object data as an input stream
     *
     * @param headers HTTP Headers
     * @param objectGroupId the object group Id
     * @param usage additional mandatory parameters usage
     * @param filename additional mandatory parameters filename
     * @param tenantId the tenant id
     * @param contractId the contract id
     * @param asyncResponse will return the inputstream
     */
    @GET
    @Path("/archiveunit/objects/download/{idOG}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("archiveunit:objects:read")
    public void getObjectAsInputStreamAsync(@Context HttpHeaders headers, @PathParam("idOG") String objectGroupId,
        @QueryParam("usage") String usage, @QueryParam("filename") String filename,
        @QueryParam("tenantId") Integer tenantId,
        @QueryParam("contractId") String contractId,
        @Suspended final AsyncResponse asyncResponse) {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncGetObjectStream(asyncResponse, objectGroupId, usage, filename, tenantId,
                contractId));
    }

    /**
     * Retrieve an Object data stored by ingest operation as an input stream
     *
     * @param headers HTTP Headers
     * @param objectId the object id to get
     * @param type of collection
     * @param asyncResponse request asynchronized response
     */
    @GET
    @Path("/ingests/{idObject}/{type}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("ingests:read")
    public void getObjectFromStorageAsInputStreamAsync(@Context HttpHeaders headers,
        @PathParam("idObject") String objectId, @PathParam("type") String type,
        @Suspended final AsyncResponse asyncResponse) {
        Integer tenantId = getTenantId(headers);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncGetObjectStorageStream(asyncResponse, objectId, type, tenantId));
    }

    private void asyncGetObjectStorageStream(AsyncResponse asyncResponse, String objectId, String type,
        Integer tenantId) {
        try (IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            IngestCollection collection = IngestCollection.valueOf(type.toUpperCase());
            Response response = client.downloadObjectAsync(objectId, collection, tenantId);
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                helper.writeResponse(Response.ok().header("Content-Disposition", "filename=" + objectId + ".xml"));
            } else {
                helper.writeResponse(Response.status(response.getStatus()));
            }
        } catch (IllegalArgumentException exc) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (final IngestExternalException exc) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }

    private void asyncGetObjectStream(AsyncResponse asyncResponse, String objectGroupId, String usage, String filename,
        Integer tenantId, String contractId) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(usage));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(filename));
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, usage);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, filename);
        } catch (final InvalidParseOperationException exc) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
            return;
        } catch (final IllegalArgumentException exc) {
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.PRECONDITION_FAILED).build());
            return;
        }
        try {
            final HashMap<String, String> emptyMap = new HashMap<>();
            final JsonNode preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(emptyMap);
            String[] usageAndVersion = usage.split("_");
            if (usageAndVersion.length != 2) {
                throw new InvalidParameterException();
            }
            UserInterfaceTransactionManager.getObjectAsInputStream(asyncResponse, preparedQueryDsl, objectGroupId,
                usageAndVersion[0], Integer.parseInt(usageAndVersion[1]), filename, tenantId, contractId);
        } catch (InvalidParseOperationException | InvalidCreateOperationException exc) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (final AccessExternalClientNotFoundException exc) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.NOT_FOUND).build());
        } catch (final AccessExternalClientServerException exc) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        } catch (final Exception exc) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }

    /***** rules Management ************/

    /**
     * @param headers HTTP header needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:rules:read")
    public Response getFileRules(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
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
        Integer tenantId = getTenantId(headers);
        if (requestIds != null) {
            requestId = requestIds.get(0);
            // get result from shiro session
            try {
                result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(sessionId, pagination));

                // save result
                PaginationHelper.setResult(sessionId, result.toJsonNode());
                // pagination
                result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(result.toJsonNode(), pagination));

                return Response.status(Status.OK).entity(result).header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                    .header(IhmDataRest.X_LIMIT, pagination.getLimit()).build();
            } catch (final VitamException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
            }
        } else {
            requestId = GUIDFactory.newRequestIdGUID(tenantId).toString();

            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
                final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);
                result = adminClient.findDocuments(AdminCollections.RULES, query,
                    getTenantId(headers));

                // save result
                PaginationHelper.setResult(sessionId, result.toJsonNode());
                // pagination
                result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(result.toJsonNode(), pagination));

                return Response.status(Status.OK).entity(result).build();
            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).build();
            } catch (final AccessExternalClientNotFoundException e) {
                LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
                return Response.status(Status.OK).entity(new RequestResponseOK<>()).build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    /**
     * @param headers HTTP Headers
     * @param ruleId id of rule
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/rules/{id_rule}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:rules:read")
    public Response getRuleById(@Context HttpHeaders headers, @PathParam("id_rule") String ruleId, String options) {
        RequestResponse result = null;

        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            ParametersChecker.checkParameter("rule Id is mandatory", ruleId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(ruleId));
            result = adminClient.findDocumentById(AdminCollections.RULES, ruleId, getTenantId(headers));
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /***
     * check the referential rules
     *
     * @param headers HTTP Headers
     * @param input the rules file csv
     * @return If the rules file is valid, return ok. If not, return the list of errors
     */
    @POST
    @Path("/rules/check")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("rules:create")
    public Response checkRefRule(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            Status status = adminClient.checkDocuments(AdminCollections.RULES, input, getTenantId(headers));
            return Response.status(status).build();
        } catch (final AccessExternalClientException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload the referential rules in the base
     *
     * @param headers HTTP Headers
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/rules/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("rules:create")
    public Response uploadRefRule(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            Status status = adminClient.createDocuments(AdminCollections.RULES, input, getTenantId(headers));
            return Response.status(status).build();
        } catch (final AccessExternalClientException e) {
            return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the action registers filtered with option query
     *
     * @param headers HTTP header needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/accession-register")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:accession-register:read")
    public Response getAccessionRegister(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
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
        Integer tenantId = getTenantId(headers);
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

            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            try {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                result = UserInterfaceTransactionManager.findAccessionRegisterSummary(options, getTenantId(headers));

                // save result
                PaginationHelper.setResult(sessionId, result.toJsonNode());
                // pagination
                result = RequestResponseOK.getFromJsonNode(PaginationHelper.getResult(result.toJsonNode(), pagination));

            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).build();
            } catch (final AccessExternalClientNotFoundException e) {
                LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
                return Response.status(Status.NOT_FOUND).build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Status.OK).entity(result).build();
        }
    }

    /**
     * Get the detail of an accessionregister matching options query
     *
     * @param headers HTTP Headers
     * @param id of accession response to get
     * @param options query criteria
     * @return accession register details
     */
    @POST
    @Path("/admin/accession-register/{id}/accession-register-detail")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("admin:accession-register:read")
    public Response getAccessionRegisterDetail(@Context HttpHeaders headers, @PathParam("id") String id,
        String options) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
        RequestResponse result = null;
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = UserInterfaceTransactionManager.findAccessionRegisterDetail(id, options, getTenantId(headers));
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * This resource returns all paths relative to a unit
     *
     * @param headers HTTP Headers
     * @param unitId the unit id
     * @param allParents all parents unit
     * @return all paths relative to a unit
     */
    @POST
    @Path("/archiveunit/tree/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("archiveunit:tree:read")
    public Response getUnitTree(@Context HttpHeaders headers, @PathParam("id") String unitId, String allParents) {

        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(allParents));
            if (allParents == null || allParents.isEmpty()) {
                return Response.status(Status.OK).entity(JsonHandler.createArrayNode()).build();
            }

            if (!JsonHandler.getFromString(allParents).isArray()) {
                throw new VitamException(INVALID_ALL_PARENTS_TYPE_ERROR_MSG);
            }

            // 1- Build DSL Query
            final ArrayNode allParentsArray = (ArrayNode) JsonHandler.getFromString(allParents);
            final List<String> allParentsList = StreamSupport.stream(allParentsArray.spliterator(), false)
                .map(p -> new String(p.asText())).collect(Collectors.toList());
            final JsonNode preparedDslQuery = DslQueryHelper.createSelectUnitTreeDSLQuery(unitId, allParentsList);

            // 2- Execute Select Query
            final RequestResponse parentsDetails = UserInterfaceTransactionManager.searchUnits(preparedDslQuery,
                getTenantId(headers));

            // 3- Build Unit tree (all paths)
            final JsonNode unitTree = UserInterfaceTransactionManager.buildUnitTree(unitId,
                parentsDetails.toJsonNode().get(UiConstants.RESULT.getResultCriteria()));

            return Response.status(Status.OK).entity(unitTree).build();
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param headers HTTP Headers
     * @param object user credentials
     * @return Response OK if login success
     */
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Context HttpHeaders headers, @Context HttpServletRequest httpRequest,
        JsonNode object) {
        final Subject subject = ThreadContext.getSubject();
        final String username = object.get("token").get("principal").textValue();
        final String password = object.get("token").get("credentials").textValue();

        if (username == null || password == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        final UsernamePasswordToken token = new UsernamePasswordToken(username, password);

        try {
            subject.login(token);
            int timeoutInSeconds = httpRequest.getSession().getMaxInactiveInterval() * 1000;
            // TODO P1 add access log
            LOGGER.info("Login success: " + username);
            List<String> permissionsByUser = PermissionReader.filterPermission(permissions, subject);

            return Response.status(Status.OK).entity(new LoginModel(username, permissionsByUser, timeoutInSeconds))
                .build();
        } catch (final Exception uae) {
            LOGGER.debug("Login fail: " + username);
            return Response.status(Status.UNAUTHORIZED).build();
        }


    }

    /**
     * returns the unit life cycle based on its id
     *
     * @param headers HTTP Headers
     * @param unitLifeCycleId the unit id (== unit life cycle id)
     * @return the unit life cycle
     */
    @GET
    @Path("/unitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("unitlifecycles:read")
    public Response getUnitLifeCycleById(@Context HttpHeaders headers, @PathParam("id_lc") String unitLifeCycleId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitLifeCycleId);
        RequestResponse result = null;
        try {
            result = UserInterfaceTransactionManager.selectUnitLifeCycleById(unitLifeCycleId, getTenantId(headers));
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final LogbookClientException e) {
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
     * @param headers HTTP Headers
     * @param objectGroupLifeCycleId the object group id (== object group life cycle id)
     * @return the object group life cycle
     */
    @GET
    @Path("/objectgrouplifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("objectgrouplifecycles:read")
    public Response getObjectGroupLifeCycleById(@Context HttpHeaders headers,
        @PathParam("id_lc") String objectGroupLifeCycleId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupLifeCycleId);
        RequestResponse result = null;

        try {
            result = UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(objectGroupLifeCycleId,
                getTenantId(headers));
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final LogbookClientException e) {
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
     * @param headers request headers
     * @return the operations list
     */
    @GET
    @Path("/operations")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("operations:read")
    public Response listOperationsDetails(@Context HttpHeaders headers) {
        try (IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            String tenantIdHeader = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
            RequestResponse<JsonNode> response = client.listOperationsDetails(Integer.parseInt(tenantIdHeader));
            return Response.status(Status.OK).entity(response).build();
        } catch (VitamClientException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update the status of an operation.
     *
     * @param headers contain X-Action and X-Context-ID
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @PUT
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("operations:update")
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
        Response response = null;
        ParametersChecker.checkParameter("ACTION Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
        String tenantIdHeader = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);

        try (IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance().getClient()) {
            response = ingestExternalClient.updateOperationActionProcess(xAction, id, Integer.parseInt(tenantIdHeader));
            final String globalExecutionState =
                response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE);
            final String globalExecutionStatus =
                response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS);

            final boolean isCompletedProcess =
                globalExecutionState != null && ProcessState.COMPLETED
                    .equals(ProcessState.valueOf(globalExecutionState));

            if (isCompletedProcess) {
                final InputStream inputStream = (InputStream) response.getEntity();
                if (inputStream != null) {
                    File file = PropertiesUtils.fileFromTmpFolder("ATR_" + id + ".xml");
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        StreamUtils.copy(inputStream, fileOutputStream);
                    } catch (IOException e) {
                        throw new VitamClientException("Error during ATR generation");
                    }
                    LOGGER.debug("DEBUG: " + file + ":" + file.length());
                    return Response.status((int) response.getStatus())
                        .entity(new VitamStreamingOutput(file, false))
                        .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .header("Content-Disposition",
                            "attachment; filename=ATR_" + id + ".xml")
                        .header(GlobalDataRest.X_REQUEST_ID, id)
                        .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, globalExecutionState)
                        .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, globalExecutionStatus)
                        .build();
                } else {
                    throw new VitamClientException("No ArchiveTransferReply found in response from Server");
                }
            }
            return Response.fromResponse(response).build();
        } catch (final ProcessingException e) {
            // if there is an unauthorized action
            LOGGER.error(e);
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        } catch (VitamClientException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(response);
        }
    }


    @DELETE
    @Path("/operations/{id}")
    @RequiresPermissions("operations:delete")
    public Response cancelProcess(@Context HttpHeaders headers, @PathParam("id") String id) {
        String tenantIdHeader = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);

        try (IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance().getClient()) {
            RequestResponse<JsonNode> resp =
                ingestExternalClient.cancelOperationProcessExecution(id, Integer.parseInt(tenantIdHeader));
            return Response.status(Status.OK).entity(resp).build();
        } catch (VitamClientException | ProcessingException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
    }

    /**
     * Upload contracts
     *
     * @param headers HTTP Headers
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/contracts")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contracts:create")
    public Response uploadRefContracts(@Context HttpHeaders headers, InputStream input) {

        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response =
                adminClient.importContracts(input, getTenantId(headers), AdminCollections.CONTRACTS);
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
     * Gets contracts
     *
     * @param headers HTTP Headers
     * @param select the query
     * @return Response
     */
    @POST
    @Path("/contracts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contracts:read")
    public Response findDocuments(@Context HttpHeaders headers, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
                RequestResponse response =
                    adminClient.findDocuments(AdminCollections.CONTRACTS, query, getTenantId(headers));
                if (response != null && response instanceof RequestResponseOK) {
                    // Récupération des hits?
                    ((RequestResponseOK) response).setHits(((RequestResponseOK) response).getResults().size(), 0, 1000);
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
     * @param id if of the contract
     * @return Response
     */
    @GET
    @Path("/contracts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contracts:read")
    public Response findContractsById(@Context HttpHeaders headers, @PathParam("id") String id) {

        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response =
                adminClient.findDocumentById(AdminCollections.CONTRACTS, id, getTenantId(headers));
            if (response != null && response instanceof RequestResponseOK) {
                // Récupération des hits?
                ((RequestResponseOK) response).setHits(((RequestResponseOK) response).getResults().size(), 0, 1000);
                return Response.status(Status.OK).entity(response).build();
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
     * Upload Access contracts
     *
     * @param headers HTTP Headers
     * @param contractId the id of ingest contract
     * @return Response
     */
    @POST
    @Path("/contracts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contracts:update")
    public Response updateEntryContracts(@Context HttpHeaders headers, @PathParam("id") String contractId,
        Map<String, String> updateData) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, contractId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(contractId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateData));

        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            final UpdateParserSingle updateParserActive = new UpdateParserSingle(new VarNameAdapter());
            Update updateStatusActive = new Update();


            if (updateData.get(STATUS_FIELD_QUERY) != null) {
                SetAction setActionStatusActive =
                    UpdateActionHelper.set(STATUS_FIELD_QUERY, updateData.get(STATUS_FIELD_QUERY));

                SetAction setActionDesactivationDateActive = null;
                if (updateData.get(ACTIVATION_DATE_FIELD_QUERY) != null) {
                    setActionDesactivationDateActive =
                        UpdateActionHelper.set(ACTIVATION_DATE_FIELD_QUERY,
                            updateData.get(ACTIVATION_DATE_FIELD_QUERY));
                } else if (updateData.get(DEACTIVATION_DATE_FIELD_QUERY) != null) {
                    setActionDesactivationDateActive = UpdateActionHelper.set(DEACTIVATION_DATE_FIELD_QUERY,
                        updateData.get(DEACTIVATION_DATE_FIELD_QUERY));
                }

                updateStatusActive.addActions(setActionStatusActive, setActionDesactivationDateActive);

            }

            if (updateData.get(ARCHIVEPROFILES_FIELD_QUERY) != null) {
                SetAction updateArchiveProfiles =
                    UpdateActionHelper.set(ARCHIVEPROFILES_FIELD_QUERY, updateData.get(ARCHIVEPROFILES_FIELD_QUERY));
                updateStatusActive.addActions(updateArchiveProfiles);

            }
            SetAction setActionLastUpdateActive =
                UpdateActionHelper.set(LAST_UPDATE_FIELD_QUERY, updateData.get(LAST_UPDATE_FIELD_QUERY));
            updateStatusActive.setQuery(QueryHelper.eq(NAME_FIELD_QUERY, updateData.get(NAME_FIELD_QUERY)));


            updateStatusActive.addActions(setActionLastUpdateActive);
            updateParserActive.parse(updateStatusActive.getFinalUpdate());
            JsonNode queryDsl = updateParserActive.getRequest().getFinalUpdate();
            final RequestResponse archiveDetails =
                adminClient.updateIngestContract(contractId, queryDsl, getTenantId(headers));
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
     * @param headers HTTP Headers
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/accesscontracts")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("accesscontracts:create")
    public Response uploadAccessContracts(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response =
                adminClient.importContracts(input, getTenantId(headers), AdminCollections.ACCESS_CONTRACTS);
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
    @RequiresPermissions("accesscontracts:read")
    public Response findAccessContract(@Context HttpHeaders headers, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
                RequestResponse response =
                    adminClient.findDocuments(AdminCollections.ACCESS_CONTRACTS, query, getTenantId(headers));
                if (response != null && response instanceof RequestResponseOK) {
                    // Récupération des hits?
                    ((RequestResponseOK) response).setHits(((RequestResponseOK) response).getResults().size(), 0, 1000);
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
     * @param headers HTTP Headers
     * @param id of the requested access contract
     * @return Response
     */
    @GET
    @Path("/accesscontracts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("accesscontracts:read")
    public Response findAccessContracts(@Context HttpHeaders headers, @PathParam("id") String id) {

        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response =
                adminClient.findDocumentById(AdminCollections.ACCESS_CONTRACTS, id, getTenantId(headers));
            if (response != null && response instanceof RequestResponseOK) {
                // Récupération des hits?
                ((RequestResponseOK) response).setHits(((RequestResponseOK) response).getResults().size(), 0, 1000);
                return Response.status(Status.OK).entity(response).build();
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
     * Upload Access contracts
     *
     * @param headers HTTP Headers
     * @param contractId the id of access contract
     * @return Response
     */
    @POST
    @Path("/accesscontracts/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("accesscontracts:update")
    public Response updateAccessContracts(@Context HttpHeaders headers, @PathParam("id") String contractId,
        Map<String, String> updateData) {
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, contractId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(contractId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateData));

        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            final UpdateParserSingle updateParserActive = new UpdateParserSingle(new VarNameAdapter());

            List<SetAction> actions = new ArrayList<>();
            actions.add(UpdateActionHelper.set(LAST_UPDATE_FIELD_QUERY, updateData.get(LAST_UPDATE_FIELD_QUERY)));
            boolean updateStatus = false;
            boolean updateEveryOriginatingAgency = false;

            if (updateData.get(STATUS_FIELD_QUERY) != null &&
                (updateData.get(ACTIVATION_DATE_FIELD_QUERY) != null ||
                    updateData.get(DEACTIVATION_DATE_FIELD_QUERY) != null)) {
                actions.add(UpdateActionHelper.set(STATUS_FIELD_QUERY, updateData.get(STATUS_FIELD_QUERY)));
                SetAction setActionDesactivationDateActive = null;
                if (updateData.get(ACTIVATION_DATE_FIELD_QUERY) != null) {
                    setActionDesactivationDateActive =
                        UpdateActionHelper.set(ACTIVATION_DATE_FIELD_QUERY,
                            updateData.get(ACTIVATION_DATE_FIELD_QUERY));
                } else if (updateData.get(DEACTIVATION_DATE_FIELD_QUERY) != null) {
                    setActionDesactivationDateActive = UpdateActionHelper.set(DEACTIVATION_DATE_FIELD_QUERY,
                        updateData.get(DEACTIVATION_DATE_FIELD_QUERY));
                }
                actions.add(setActionDesactivationDateActive);
                updateStatus = true;
            } else if (updateData.get(STATUS_FIELD_QUERY) != null ||
                updateData.get(ACTIVATION_DATE_FIELD_QUERY) != null ||
                updateData.get(DEACTIVATION_DATE_FIELD_QUERY) != null) {
                return Response.status(Status.BAD_REQUEST).entity("Invalid update status query").build();
            }

            if (updateData.get(EVERY_ORIG_AGENCY_FIELD_QUERY) != null) {
                Boolean everyOrigAgency = BooleanUtils.toBooleanObject(updateData.get(EVERY_ORIG_AGENCY_FIELD_QUERY));
                if (everyOrigAgency == null) {
                    return Response.status(Status.BAD_REQUEST).entity("Invalid EveryOriginatingAgency value").build();
                }
                actions.add(UpdateActionHelper.set(EVERY_ORIG_AGENCY_FIELD_QUERY, everyOrigAgency));
                updateEveryOriginatingAgency = true;
            }

            if (!updateStatus && !updateEveryOriginatingAgency) {
                return Response.status(Status.BAD_REQUEST).entity("Empty Query, Nothing to Update").build();
            }

            Update updateStatusActive = new Update();
            updateStatusActive.setQuery(QueryHelper.eq(NAME_FIELD_QUERY, updateData.get(NAME_FIELD_QUERY)));
            updateStatusActive.addActions(actions.toArray(new SetAction[actions.size()]));
            updateParserActive.parse(updateStatusActive.getFinalUpdate());
            JsonNode queryDsl = updateParserActive.getRequest().getFinalUpdate();
            final RequestResponse archiveDetails =
                adminClient.updateAccessContract(contractId, queryDsl, getTenantId(headers));
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
     * upload context
     *
     * @param headers HTTP Headers
     * @param input the file json
     * @return Response
     */
    @POST
    @Path("/contexts")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("contexts:create")
    public Response uploadContext(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response =
                adminClient.importContexts(input, getTenantId(headers));
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
     * Create profiles metadata
     *
     * @param headers HTTP Headers
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/profiles")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:create")
    public Response createProfilesMetadata(@Context HttpHeaders headers, InputStream input)
        throws IOException {
        // want a creation
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response =
                adminClient.createProfiles(input, getTenantId(headers));
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
     * Upload profile xsd or rng
     *
     * @param headers HTTP Headers
     * @param input the format file CSV
     * @return Response
     */
    @PUT
    @Path("/profiles/{id}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:create")
    public Response importProfileFile(@Context HttpHeaders headers, InputStream input, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response =
                adminClient.importProfileFile(id, input, getTenantId(headers));
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

    @GET
    @Path("/profiles/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("profiles:read")
    public void downloadProfileFile(@Context HttpHeaders headers, @PathParam("id") String profileMetadataId,
        @Suspended final AsyncResponse asyncResponse) {

        ParametersChecker.checkParameter("Profile id should be filled", profileMetadataId);

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncDownloadProfileFile(profileMetadataId, getTenantId(headers), asyncResponse));
    }

    private void asyncDownloadProfileFile(String profileMetadataId, int tenant, final AsyncResponse asyncResponse) {

        AsyncInputStreamHelper helper;

        try (AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient()) {

            final Response response = client.downloadProfileFile(profileMetadataId, tenant);
            helper = new AsyncInputStreamHelper(asyncResponse, response);
            final Response.ResponseBuilder responseBuilder =
                Response.status(Status.OK)
                    .header("Content-Disposition", response.getHeaderString("Content-Disposition"))
                    .type(response.getMediaType());
            helper.writeResponse(responseBuilder);
        } catch (final AccessExternalNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            asyncResponseResume(asyncResponse,
                Response.status(Status.NOT_FOUND)
                    .entity(getErrorEntity(Status.NOT_FOUND, exc.getMessage()).toString()).build());
        } catch (final AccessExternalClientException exc) {
            LOGGER.error(exc.getMessage(), exc);
            asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exc.getMessage()).toString()).build());
        }
    }

    private VitamError getErrorEntity(Status status, String msgErr) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(msgErr);
    }

    /**
     * Query to get profiles
     *
     * @param headers HTTP Headers
     * @param select the query to find access contracts
     * @return Response
     */
    @POST
    @Path("/profiles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:read")
    public Response findProfiles(@Context HttpHeaders headers, String select) {
        try {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);

            try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
                RequestResponse response =
                    adminClient.findDocuments(AdminCollections.PROFILE, query, getTenantId(headers));
                if (response != null && response instanceof RequestResponseOK) {
                    // Récupération des hits?
                    ((RequestResponseOK) response).setHits(((RequestResponseOK) response).getResults().size(), 0, 1000);
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
     * @param headers HTTP Headers
     * @param id of the requested access contract
     * @return Response
     */
    @GET
    @Path("/profiles/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("profiles:read")
    public Response findProfileByID(@Context HttpHeaders headers, @PathParam("id") String id) {

        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response =
                adminClient.findDocumentById(AdminCollections.PROFILE, id, getTenantId(headers));
            if (response != null && response instanceof RequestResponseOK) {
                // Récupération des hits?
                ((RequestResponseOK) response).setHits(((RequestResponseOK) response).getResults().size(), 0, 1000);
                return Response.status(Status.OK).entity(response).build();
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
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        return tenantId;
    }

    private File downloadAndSaveATR(String guid, Integer tenantId)
        throws VitamClientException, IngestExternalException {
        File file = null;
        Response response = null;
        try (IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance().getClient()) {
            response = ingestExternalClient
                .downloadObjectAsync(guid, IngestCollection.REPORTS, tenantId);
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
     * @param headers default headers received from Front side (TenantId, user ...)
     * @param operationCriteria a DSLQuery to find the TRACEABILITY operation to verify
     * @return TRACEABILITY check process : the logbookOperation created during this process
     */
    @POST
    @Path("/traceability/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("traceability:check:create")
    public Response checkOperationTraceability(@Context HttpHeaders headers, String operationCriteria) {

        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, operationCriteria);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(operationCriteria));

            // Get tenantId value
            Integer tenantIdHeader = getTenantId(headers);

            // Prepare DSLQuery based on the received criteria
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(operationCriteria);
            final JsonNode dslQuery = DslQueryHelper.createSingleQueryDSL(optionsMap);

            // Start check process
            RequestResponse<JsonNode> result =
                UserInterfaceTransactionManager.checkTraceabilityOperation(dslQuery, tenantIdHeader);

            // By default the returned status is different from the result of the verification process because we are
            // returning the report
            return Response.status(Status.OK).entity(result).build();

        } catch (AccessExternalClientServerException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.VITAM.getName())
                .setState(CODE_VITAM)
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.VITAM.getName())
                .setState(CODE_VITAM)
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(e);
            final Status status = Status.UNAUTHORIZED;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.VITAM.getName())
                .setState(CODE_VITAM)
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        }
    }


    /**
     * Download the Traceability Operation file
     *
     * @param headers request headers
     * @param operationId the TRACEABILITY operation identifier
     * @param contractId the contractId
     * @param asyncResponse the async response
     * @return a response containing the file name stream
     */
    @GET
    @Path("/traceability/{idOperation}/content")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequiresPermissions("traceability:content:read")
    public void downloadTraceabilityFile(@Context HttpHeaders headers, @PathParam("idOperation") String operationId,
        @QueryParam("contractId") String contractId,
        @Suspended final AsyncResponse asyncResponse) {

        // Check parameters
        ParametersChecker.checkParameter("Operation Id should be filled", operationId);

        // Get tenantId from headers

        Integer tenantId = getTenantId(headers);
        VitamThreadUtils.getVitamSession().setContractId(contractId);
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> downloadTraceabilityFileAsync(asyncResponse, operationId, tenantId, contractId));
    }

    private void downloadTraceabilityFileAsync(final AsyncResponse asyncResponse, String operationId,
        Integer tenantId, String contractId) {

        Response response = null;
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {

            response = client.downloadTraceabilityOperationFile(operationId, tenantId, contractId);

            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);

            if (response.getStatus() == Status.OK.getStatusCode()) {
                helper.writeResponse(
                    Response.ok().header("Content-Disposition", response.getHeaderString("Content-Disposition")));
            } else {
                helper.writeResponse(Response.status(response.getStatus()));
            }
        } catch (IllegalArgumentException exc) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (AccessExternalClientServerException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
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
                JsonNode jsonNode =
                    UserInterfaceTransactionManager.extractInformationFromTimestamp(optionsMap.get("timestamp"));
                return Response.status(Status.OK).entity(jsonNode).build();
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }
        } catch (BadRequestException | InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
}
