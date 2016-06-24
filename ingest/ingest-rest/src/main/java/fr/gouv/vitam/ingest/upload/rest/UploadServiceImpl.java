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
package fr.gouv.vitam.ingest.upload.rest;

import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.api.upload.UploadService;
import fr.gouv.vitam.ingest.model.UploadResponseDTO;
import fr.gouv.vitam.ingest.upload.core.UploadSipHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

/**
 * UploadServiceImpl implements UploadService
 *
 */
@Path("/ingest/v1")
@Consumes("application/json")
@Produces("application/json")
@javax.ws.rs.ApplicationPath("webresources")
public class UploadServiceImpl implements UploadService {
    // FIXME REVIEW Comment

    private static VitamLogger VITAM_LOGGER = VitamLoggerFactory.getInstance(UploadServiceImpl.class);

    private static final String FOLDER_SIP = "SIP";
    private Properties properties = null;

    private LogbookParameters parameters;
    private LogbookClient logBookClient;
    private ProcessingManagementClient processingClient;
    private WorkspaceClient workspaceClient;

    /**
     * Empty constructor
     * 
     * @throws VitamException
     */
    public UploadServiceImpl(Properties properties) throws VitamException {
        this.properties = properties;
    }

    /**
     * @throws VitamException if loading properties ingest-rest.properties failed
     */
    public UploadServiceImpl(LogbookClient logbookClient, ProcessingManagementClient processingManagementClient,
        WorkspaceClient wksClient, Properties properties) throws VitamException {
        logBookClient = logbookClient;
        processingClient = processingManagementClient;
        workspaceClient = wksClient;
        this.properties = properties;
    }

    /**
     *
     * @param uploadedInputStream
     * @param fileDetail
     * @return
     * @throws VitamException
     */
    @Override
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSipAsStream(@FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail) throws VitamException {

        if (uploadedInputStream == null || fileDetail == null) {
            VITAM_LOGGER.error("input stream null");
            // TODO commentaire à variabiliser (FR/EN)
            throw new VitamException("error input stream");
        }


        final int tenantId = 0; // default tenanId
        Response response = null;
        String url = "";
        String containerName = "";

        try {
            VITAM_LOGGER.info("Starting up the save file sip : " + fileDetail.getFileName());

            // guid generated for the container in the workspace
            final GUID containerGUID = GUIDFactory.newOperationIdGUID(tenantId);
            containerName = containerGUID.getId();

            if (properties != null) {
                final String workspaceUrl = getWorkspaceUrl(properties);

                VITAM_LOGGER.info(workspaceUrl);
                url = workspaceUrl;
            }

            final GUID ingestGuid = GUIDFactory.newOperationIdGUID(tenantId);
            logBookClient = logbookInitialisation(ingestGuid, containerGUID, tenantId);

            // workspace
            pushSipStreamToWorkspace(url, containerName, uploadedInputStream, parameters);

            // processing
            logBookClient = callProcessingEngine(parameters, logBookClient, containerName);

            final UploadResponseDTO uploadResponseDTO =
                UploadSipHelper.getUploadResponseDTO(fileDetail.getFileName(), 200, "success",
                    "201", "success", "200", "success");
            response = Response.ok(uploadResponseDTO, "application/json").build();

        } catch (final ContentAddressableStorageServerException e) {

            if (parameters != null) {
                parameters.setStatus(LogbookOutcome.ERROR);
                parameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    "500_123456"); // 404 = code http, 123456 = code erreur Vitam
                try {
                    logBookClient = callLogBookUpdate(logBookClient, parameters);
                } catch (final LogbookClientNotFoundException e1) {
                    VITAM_LOGGER.error(e1.getMessage(), e1);
                } catch (final LogbookClientBadRequestException e1) {
                    VITAM_LOGGER.error(e1.getMessage(), e1);
                } catch (final LogbookClientServerException e1) {
                    VITAM_LOGGER.error(e1.getMessage(), e1);
                }
            }

            VITAM_LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            final UploadResponseDTO uploadResponseDTO =
                UploadSipHelper.getUploadResponseDTO(fileDetail.getFileName(), 500, e.getMessage(),
                    "500", "workspace failed", "500", "error workspace");
            response = Response.ok(uploadResponseDTO, "application/json").build();
        } catch (final Exception e) {

            if (parameters != null) {
                parameters.setStatus(LogbookOutcome.ERROR);
                parameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    "500_123456"); // 404 = code http, 123456 = code erreur Vitam
                try {
                    logBookClient = callLogBookUpdate(logBookClient, parameters);
                } catch (final LogbookClientNotFoundException e1) {
                    VITAM_LOGGER.error(e1.getMessage(), e1);
                } catch (final LogbookClientBadRequestException e1) {
                    VITAM_LOGGER.error(e1.getMessage(), e1);
                } catch (final LogbookClientServerException e1) {
                    VITAM_LOGGER.error(e1.getMessage(), e1);
                }
            }

            VITAM_LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            final UploadResponseDTO uploadResponseDTO =
                UploadSipHelper.getUploadResponseDTO(fileDetail.getFileName(), 500, e.getMessage(),
                    "500", "upload failed", "500", "error ingest");
            response = Response.ok(uploadResponseDTO, "application/json").build();

        } finally {
            if (logBookClient != null) {
                logBookClient.close();
            }
        }
        return response;
    }


    private String getWorkspaceUrl(Properties properties) {
        String uri = "";

        if (properties != null) {
            final String workspaceProtocol = properties.getProperty("ingest.core.workspace.client.protocol");
            final String workspaceHost = properties.getProperty("ingest.core.workspace.client.host");
            final String workspacePort = properties.getProperty("ingest.core.workspace.client.port");

            uri = workspaceProtocol + "://" + workspaceHost + ":" + workspacePort;
        }
        return uri;
    }

    private String getProcessingUrl(Properties properties) {
        String uri = "";

        if (properties != null) {
            final String processingProtocol = properties.getProperty("ingest.core.processing.client.protocol");
            final String processingHost = properties.getProperty("ingest.core.processing.client.host");
            final String processingPort = properties.getProperty("ingest.core.processing.client.port");

            uri = processingProtocol + "://" + processingHost + ":" + processingPort;
        }

        return uri;
    }

    /**
     *
     * @param urlWorkspace
     * @param containerName
     * @param uploadedInputStream
     */
    private void pushSipStreamToWorkspace(final String urlWorkspace, final String containerName,
        final InputStream uploadedInputStream, final LogbookParameters parameters)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageAlreadyExistException {

        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "Try to push stream to workspace...");
        VITAM_LOGGER.info("Try to push stream to workspace...");

        // call workspace
        if (workspaceClient == null) {
            workspaceClient = new WorkspaceClient(urlWorkspace);
        }
        if (!workspaceClient.isExistingContainer(containerName)) {
            workspaceClient.createContainer(containerName);
            workspaceClient.unzipObject(containerName, FOLDER_SIP, uploadedInputStream);
        }

        VITAM_LOGGER.info(" -> push stream to workspace finished");
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "-> push stream to workspace finished");
    }


    private LogbookClient callProcessingEngine(final LogbookParameters parameters, final LogbookClient client,
        final String containerName) throws InvalidParseOperationException,
        ProcessingException, LogbookClientNotFoundException, LogbookClientBadRequestException,
        LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "Try to call processing...");
        VITAM_LOGGER.info("Try to call processing...");
        final String workflowId = "DefaultIngestWorkflow";

        final String workspaceUrl = getProcessingUrl(properties);
        final String urlProcessing = workspaceUrl;

        processingClient = new ProcessingManagementClient(urlProcessing);
        final String processingRetour = processingClient.executeVitamProcess(containerName, workflowId);
        VITAM_LOGGER.info(" -> process workflow finished " + processingRetour);
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "-> process workflow finished");
        return callLogBookUpdate(client, parameters);
    }



    // TODO ???? Le Factory le fait pour toi !! Cela me semble incorrect!
    // TODO Supprimer parameters car tu ne l'utilises pas (et donc dans le property les paramètres associés).
    private LogbookClient logbookInitialisation(final GUID ingestGuid, final GUID containerGUID, int tenantId)
        throws LogbookClientNotFoundException,
        LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException {

        final GUID eventIdentifier = ingestGuid; // Event GUID
        final String eventType = "Process_SIP_unitary"; // Event Type
        final GUID eventIdentifierProcess = containerGUID; // Event Identifier Process
        final GUID eventIdentifierRequest = ingestGuid; // X-Request-Id
        final GUID outcomeDetailMessage = ingestGuid; // "SIP entry : " + fileDetail.getFileName(); // Outcome detail
                                                      // message

        VITAM_LOGGER.debug("call journal...");
        // Récupération de la classe paramètre avec argument
        parameters = buildLogBookParameters(eventIdentifier, eventIdentifierProcess, eventIdentifierRequest,
            outcomeDetailMessage, eventType);

        final String server = properties.getProperty("ingest.core.logbook.server");
        final String sPort = properties.getProperty("ingest.core.logbook.port");
        final int port = Integer.parseInt(sPort);
        final LogbookClient client = callLogBookCreate(containerGUID.getId(), tenantId, parameters, server, port);
        return client;
    }


    /**
     *
     *
     *
     * @param containerGuid X-Request-Id, Global Object Id: in ingest = SIP GUID
     * @param tenantId TenantId
     * @throws LogbookClientNotFoundException
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientServerException
     */
    private LogbookClient callLogBookCreate(String containerGuid, int tenantId, LogbookParameters parameters,
        String server, int port)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException,
        LogbookClientAlreadyExistsException {

        final LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
        client.create(parameters);

        return client;
    }

    /**
     *
     * @param eventIdentifier
     * @param eventIdentifierProcess
     * @param eventIdentifierRequest
     * @param eventType
     * @param outcomeDetailMessage
     * @return
     */
    private LogbookParameters buildLogBookParameters(final GUID eventIdentifier, final GUID eventIdentifierProcess,
        final GUID outcomeDetailMessage, final GUID eventIdentifierRequest, final String eventType) {

        parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eventIdentifier, eventType, eventIdentifierProcess,
            LogbookTypeProcess.INGEST, LogbookOutcome.STARTED,
            outcomeDetailMessage != null ? outcomeDetailMessage.toString() : "outcomeDetailMessage",
            eventIdentifierRequest);

        final StringBuilder logBookParams = new StringBuilder();
        logBookParams.append("Logbook parameters : {")
            .append("eventIdentifier=").append(parameters.getParameterValue(LogbookParameterName.eventIdentifier))
            .append(", ")
            .append("eventType=").append(parameters.getParameterValue(LogbookParameterName.eventType)).append(", ")
            .append("eventIdentifierProcess=")
            .append(parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess)).append(", ")
            .append("eventTypeProcess=").append(parameters.getParameterValue(LogbookParameterName.eventTypeProcess))
            .append(", ")
            .append("outcome=").append(parameters.getParameterValue(LogbookParameterName.outcome)).append(", ")
            .append("eventIdentifierRequest=")
            .append(parameters.getParameterValue(LogbookParameterName.eventIdentifierRequest)).append(", ")
            .append("outcomeDetailMessage=")
            .append(parameters.getParameterValue(LogbookParameterName.outcomeDetailMessage)).append(" }");

        VITAM_LOGGER.debug("Logbook parameters : " + logBookParams.toString());

        return parameters;
    }

    private LogbookClient callLogBookUpdate(LogbookClient client, LogbookParameters parameters)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {

        parameters.setStatus(LogbookOutcome.OK);

        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "update logbook");
        client.update(parameters);

        return client;
    }

    /**
     *
     * Upload service a received SIP from a SIA with a name associated to the SIP TODO for another iteration
     *
     * @param uploadedInputStream
     * @param fileDetail
     * @param sipName
     * @return
     * @throws VitamException
     */
    @Override
    @POST
    @Path("/uploadName")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSipAsStream(@FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail, @FormParam("name") String sipName)
        throws VitamException {
        final Response response = uploadSipAsStream(uploadedInputStream, fileDetail);
        return response;
    }


    @Override
    @GET
    @Path("/status")
    public Response status() {
        return Response.status(200).entity("").build();
    }
}
