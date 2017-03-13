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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
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

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResource.class);
    private static final String BAD_REQUEST_EXCEPTION_MSG = "Bad request Exception";
    private static final String ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Access client unavailable";
    private static final String ACCESS_SERVER_EXCEPTION_MSG = "Access Server exception";
    private static final String INTERNAL_SERVER_ERROR_MSG = "INTERNAL SERVER ERROR";
    private static final String SEARCH_CRITERIA_MANDATORY_MSG = "Search criteria payload is mandatory";
    private static final String FIELD_ID_KEY = "fieldId";
    private static final String NEW_FIELD_VALUE_KEY = "newFieldValue";
    private static final String INVALID_ALL_PARENTS_TYPE_ERROR_MSG = "The parameter \"allParents\" is not an array";

    private static final String LOGBOOK_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Logbook Client NOT FOUND Exception";
    private static final ConcurrentMap<String, List<Object>> uploadRequestsStatus = new ConcurrentHashMap<>();
    private static final int COMPLETE_RESPONSE_SIZE = 3;
    private static final int GUID_INDEX = 0;
    private static final int RESPONSE_STATUS_INDEX = 1;
    private static final int ATR_CONTENT_INDEX = 2;
    private static final String DEFAULT_CONTEXT = "defaultContext";

    private static final String FLOW_TOTAL_CHUNKS_HEADER = "FLOW-TOTAL-CHUNKS";
    private static final String FLOW_CHUNK_NUMBER_HEADER = "FLOW-CHUNK-NUMBER";

    private final WebApplicationConfig webApplicationConfig;

    /**
     * Constructor
     *
     * @param webApplicationConfig
     */
    public WebApplicationResource(WebApplicationConfig webApplicationConfig) {
        super(new BasicVitamStatusServiceImpl(), webApplicationConfig.getTenants());
        this.webApplicationConfig = webApplicationConfig;
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
     * @param headers header needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param criteria criteria search for units
     * @return Reponse
     */
    @POST
    @Path("/archivesearch/units")
    @Produces(MediaType.APPLICATION_JSON)
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
                final Map<String, String> criteriaMap = JsonHandler.getMapStringFromString(criteria);
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
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    /**
     * @param headers
     * @param unitId archive unit id
     * @return archive unit details
     */
    @GET
    @Path("/archivesearch/unit/{id}")
    @Produces(MediaType.APPLICATION_JSON)
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
                final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
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
     * @param headers
     * @param operationId id of operation
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/logbook/operations/{idOperation}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResultById(@Context HttpHeaders headers, @PathParam("idOperation") String operationId,
        String options) {
        RequestResponse result = null;
        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = UserInterfaceTransactionManager.selectOperationbyId(operationId, getTenantId(headers));
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
     * @param request
     * @param response
     * @param stream data input stream for the current chunk
     * @param headers HTTP Headers containing chunk information
     * @return Response
     */
    @Path("ingest/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @Context HttpHeaders headers, InputStream stream) {
        String operationGuidFirstLevel = null;
        File temporarySipFile = null;
        try {
            // Received chunk index
            final int currentChunkIndex = Integer.parseInt(headers.getHeaderString(FLOW_CHUNK_NUMBER_HEADER));

            // Total number of chunks
            final int totalChunks = Integer.parseInt(headers.getHeaderString(FLOW_TOTAL_CHUNKS_HEADER));

            String contextId = headers.getHeaderString(GlobalDataRest.X_CONTEXT_ID);
            String action = headers.getHeaderString(GlobalDataRest.X_ACTION);

            if (currentChunkIndex == 1) {
                // GUID operation (Server Application level)
                operationGuidFirstLevel = GUIDFactory.newGUID().getId();
                temporarySipFile = PropertiesUtils.fileFromTmpFolder(operationGuidFirstLevel);

                // Write the first chunk
                try (FileOutputStream outputStream = new FileOutputStream(temporarySipFile)) {
                    StreamUtils.copy(stream, outputStream);
                }
            } else {
                operationGuidFirstLevel = headers.getHeaderString(GlobalDataRest.X_REQUEST_ID);
                temporarySipFile = PropertiesUtils.fileFromTmpFolder(operationGuidFirstLevel);

                // Append the received chunk to the temporary file
                try (final FileOutputStream outputStream = new FileOutputStream(temporarySipFile, true)) {
                    StreamUtils.copy(stream, outputStream);
                }
            }

            // if it is the last chunk => start INGEST upload
            if (currentChunkIndex == totalChunks) {
                startUpload(operationGuidFirstLevel, getTenantId(headers), contextId, action);
            }
        } catch (final IOException e) {
            LOGGER.error("Upload failed", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final RuntimeException e) {
            LOGGER.error("Upload failed", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        try {
            return Response
                .status(Status.OK).entity(JsonHandler.getFromString(
                    "{\"" + GlobalDataRest.X_REQUEST_ID.toLowerCase() + "\":\"" + operationGuidFirstLevel + "\"}"))
                .build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Upload failed", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
            Response finalResponse = null;
            File file = null;
            final File temporarSipFile = PropertiesUtils.fileFromTmpFolder(operationGuidFirstLevel);
            try (IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
                try {
                    finalResponse = client.upload(new FileInputStream(temporarSipFile), tenantId, contextId, action);
                    final String guid = finalResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
                    final String globalExecutionStatus =
                        finalResponse.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS);

                    final boolean isCompletedProcess =
                        globalExecutionStatus != null && (ProcessExecutionStatus.COMPLETED
                            .equals(ProcessExecutionStatus.valueOf(globalExecutionStatus)) ||
                            ProcessExecutionStatus.FAILED
                                .equals(ProcessExecutionStatus.valueOf(globalExecutionStatus)));

                    if (isCompletedProcess) {
                        final InputStream inputStream = (InputStream) finalResponse.getEntity();
                        if (inputStream != null) {
                            file = PropertiesUtils.fileFromTmpFolder("ATR_" + guid + ".xml");
                            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                                StreamUtils.copy(inputStream, fileOutputStream);
                            }
                            final List<Object> finalResponseDetails = new ArrayList<>();
                            finalResponseDetails.add(guid);
                            finalResponseDetails.add(finalResponse.getStatus());
                            finalResponseDetails.add(file);
                            LOGGER.debug("DEBUG: " + file + ":" + file.length());
                            uploadRequestsStatus.put(operationGuidFirstLevel, finalResponseDetails);
                        } else {
                            throw new VitamClientException("No ArchiveTransferReply found in response from Server");
                        }
                    } else {
                        final List<Object> finalResponseDetails = new ArrayList<>();
                        finalResponseDetails.add(guid);
                        finalResponseDetails.add(Status.fromStatusCode(finalResponse.getStatus()));
                        uploadRequestsStatus.put(operationGuidFirstLevel, finalResponseDetails);
                    }
                } finally {
                    DefaultClient.staticConsumeAnyEntityAndClose(finalResponse);
                }
            } catch (IOException | VitamException e) {
                LOGGER.error("Upload failed", e);
                final List<Object> finalResponseDetails = new ArrayList<>();
                finalResponseDetails.add(operationGuidFirstLevel);
                finalResponseDetails.add(Status.INTERNAL_SERVER_ERROR);
                uploadRequestsStatus.put(operationGuidFirstLevel, finalResponseDetails);
                if (file != null) {
                    file.delete();
                }
            } finally {
                if (temporarSipFile != null) {
                    temporarSipFile.delete();
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
    public Response checkUploadOperation(@PathParam("id_op") String operationId) {
        // TODO Need a tenantId test for checking upload (Only IHM-DEMO scope,
        // dont call VITAM backend) ?
        // 1- Check if the requested operation is done
        final List<Object> responseDetails = uploadRequestsStatus.get(operationId);
        if (responseDetails != null) {
            if (responseDetails.size() == COMPLETE_RESPONSE_SIZE) {
                final File file = (File) responseDetails.get(ATR_CONTENT_INDEX);
                LOGGER.debug("DEBUG: " + file + ":" + file.length());
                return Response.status((int) responseDetails.get(RESPONSE_STATUS_INDEX))
                    .entity(new VitamStreamingOutput(file, false)).type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header("Content-Disposition",
                        "attachment; filename=ATR_" + responseDetails.get(GUID_INDEX) + ".xml")
                    .header(GlobalDataRest.X_REQUEST_ID, responseDetails.get(GUID_INDEX)).build();
            } else {
                return Response.status((Status) responseDetails.get(RESPONSE_STATUS_INDEX))
                    .header(GlobalDataRest.X_REQUEST_ID, responseDetails.get(GUID_INDEX)).build();
            }
        }

        // 2- Return the created GUID
        return Response.status(Status.NO_CONTENT).header(GlobalDataRest.X_REQUEST_ID, operationId).build();
    }

    /**
     * Once done, clear the Upload operation history
     *
     * @param operationId
     * @return the Response
     */
    @Path("clear/{id_op}")
    @GET
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
            // Cleaning process succeeded
            return Response.status(Status.OK).header(GlobalDataRest.X_REQUEST_ID, operationId).build();
        } else {
            // Cleaning process failed
            return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, operationId).build();
        }
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
            final RequestResponse archiveDetails = UserInterfaceTransactionManager.updateUnits(preparedQueryDsl, unitId,
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
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param headers HTTP header needed for the request: X-TENANT-ID (mandatory), X-LIMIT/X-OFFSET (not mandatory)
     * @param sessionId json session id from shiro
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/formats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileFormats(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
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
	            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
	            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);
	            result = adminClient.findDocuments(AdminCollections.FORMATS, query,
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
	            return Response.status(Status.NOT_FOUND).build();
	        } catch (final Exception e) {
	            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
	            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	        }
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
    public Response checkRefFormat(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            Response response = adminClient.checkDocuments(AdminCollections.FORMATS, input, getTenantId(headers));
            switch (response.getStatusInfo().getFamily()) {
                case SUCCESSFUL:
                    return Response.status(Status.OK).build();
                default:
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
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
    public Response uploadRefFormat(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            Response response = adminClient.createDocuments(AdminCollections.FORMATS, input, getTenantId(headers));
            switch (response.getStatusInfo().getFamily()) {
                case SUCCESSFUL:
                    return Response.status(Status.OK).build();
                default:
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
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
     * @param version additional mandatory parameters version
     * @param filename additional mandatory parameters filename
     * @param tenantId the tenant id
     * @param asyncResponse will return the inputstream
     *
     */
    @GET
    @Path("/archiveunit/objects/download/{idOG}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectAsInputStreamAsync(@Context HttpHeaders headers, @PathParam("idOG") String objectGroupId,
        @QueryParam("usage") String usage, @QueryParam("version") String version,
        @QueryParam("filename") String filename, @QueryParam("tenantId") Integer tenantId,
        @Suspended final AsyncResponse asyncResponse) {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncGetObjectStream(asyncResponse, objectGroupId, usage, version, filename, tenantId));
    }

    /**
     * Retrieve an Object data stored by ingest operation as an input stream
     *
     * @param headers HTTP Headers
     * @param objectId
     * @param type
     * @param asyncResponse
     */
    @GET
    @Path("/ingests/{idObject}/{type}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
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
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (final IngestExternalException exc) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, exc);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }

    private void asyncGetObjectStream(AsyncResponse asyncResponse, String objectGroupId, String usage, String version,
        String filename, Integer tenantId) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(version));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(usage));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(filename));
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, usage);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, version);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, filename);
        } catch (final InvalidParseOperationException exc) {
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, Response.status(Status.BAD_REQUEST).build());
            return;
        } catch (final IllegalArgumentException exc) {
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.PRECONDITION_FAILED).build());
            return;
        }
        try {
            final HashMap<String, String> emptyMap = new HashMap<>();
            final JsonNode preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(emptyMap);
            UserInterfaceTransactionManager.getObjectAsInputStream(asyncResponse, preparedQueryDsl, objectGroupId,
                usage, Integer.parseInt(version), filename, tenantId);
        } catch (InvalidParseOperationException | InvalidCreateOperationException exc) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, Response.status(Status.BAD_REQUEST).build());
        } catch (final AccessExternalClientNotFoundException exc) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, Response.status(Status.NOT_FOUND).build());
        } catch (final AccessExternalClientServerException exc) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, exc);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        } catch (final Exception exc) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, exc);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
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
	            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
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
	            return Response.status(Status.NOT_FOUND).build();
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
    public Response checkRefRule(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            Response response = adminClient.checkDocuments(AdminCollections.RULES, input, getTenantId(headers));
            switch (response.getStatusInfo().getFamily()) {
                case SUCCESSFUL:
                    return Response.status(Status.OK).build();
                default:
                    return Response.status(Status.BAD_REQUEST).build();
            }
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
    public Response uploadRefRule(@Context HttpHeaders headers, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            Response response = adminClient.createDocuments(AdminCollections.RULES, input, getTenantId(headers));
            switch (response.getStatusInfo().getFamily()) {
                case SUCCESSFUL:
                    return Response.status(Status.OK).build();
                default:
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
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
     * @param id
     * @param options query criteria
     * @return accession register details
     */
    @POST
    @Path("/admin/accession-register/{id}/accession-register-detail")
    @Produces(MediaType.APPLICATION_JSON)
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
    public Response login(@Context HttpHeaders headers, JsonNode object) {
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
     * returns the unit life cycle based on its id
     *
     * @param headers HTTP Headers
     * @param unitLifeCycleId the unit id (== unit life cycle id)
     * @return the unit life cycle
     */
    @GET
    @Path("/unitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
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
    public Response listOperationsDetails(@Context HttpHeaders headers) {
        Response response = null;
        try (IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            String tenantIdHeader = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
            VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(tenantIdHeader));
            response = client.listOperationsDetails();
            return Response.fromResponse(response).build();
        } catch (VitamClientException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update the status of an operation.
     *
     * @param headers contain X-Action and X-Context-ID
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @param id operation identifier
     * @param asyncResponse asyncResponse
     * @return http response
     */
    @Path("operations/{id}")
    @PUT
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
        Response response = null;
        ParametersChecker.checkParameter("ACTION Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
        String tenantIdHeader = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(tenantIdHeader));

        try (IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance().getClient()) {
            response = ingestExternalClient.updateOperationActionProcess(xAction, id);
            final String globalExecutionStatus =
                response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS);

            final boolean isCompletedProcess =
                globalExecutionStatus != null && (ProcessExecutionStatus.COMPLETED
                    .equals(ProcessExecutionStatus.valueOf(globalExecutionStatus)) ||
                    ProcessExecutionStatus.FAILED
                        .equals(ProcessExecutionStatus.valueOf(globalExecutionStatus)));

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
                        .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, globalExecutionStatus)
                        .build();
                } else {
                    throw new VitamClientException("No ArchiveTransferReply found in response from Server");
                }
            }
        } catch (final ProcessingException e) {
            // if there is an unauthorized action
            LOGGER.error(e);
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        } catch (InternalServerException | VitamClientException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return response;
    }


    @DELETE
    @Path("/operations/{id}")
    public Response cancelProcess(@Context HttpHeaders headers, @PathParam("id") String id) {
        String tenantIdHeader = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        VitamThreadUtils.getVitamSession().setTenantId(Integer.parseInt(tenantIdHeader));

        try (IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance().getClient()) {

            return ingestExternalClient.cancelOperationProcessExecution(id);
            // ItemStatus itemStatus =
            // ingestExternalClient.cancelOperationProcessExecution(id);
            //
            // // TODO check null value
            // return Response.status(Status.OK)
            // .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, itemStatus.getGlobalExecutionStatus().toString())
            // .build();
        } catch (InternalServerException | VitamClientException | ProcessingException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
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

}
