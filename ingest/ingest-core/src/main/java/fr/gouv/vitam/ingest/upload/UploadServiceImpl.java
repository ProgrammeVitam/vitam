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
package fr.gouv.vitam.ingest.upload;

import java.io.IOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.api.response.IngestResponse;
import fr.gouv.vitam.ingest.core.exception.IngestException;
import fr.gouv.vitam.ingest.model.UploadResponseDTO;
import fr.gouv.vitam.ingest.util.PropertyUtil;
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
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

@Path("/ingest")
@Consumes("application/json")
@Produces("application/json")
@javax.ws.rs.ApplicationPath("webresources")
public class UploadServiceImpl extends AbstractService implements UploadService {
    // FIXME REVIEW Comment

    private static VitamLogger VITAM_LOGGER = VitamLoggerFactory.getInstance(UploadServiceImpl.class);
    private static Logger LOGGER = LoggerFactory.getLogger(UploadServiceImpl.class);
    // FIXME REVIEW Remove non Vitam Logger and Fix POM


    private static final String INGEST_MODULE_DIR = "ingest-core";
    public static final String PROPERTIES_CORE = "ingest-core.properties";
    private static final String URI_WORKSPACE = "ingest.core.workspace.client.uri";
    private static final String URI_PROCESSING = "ingest.core.processing.uri";
    private static final String URL_WORKSPACE = "workspace.client.url";
    private Properties properties = null;

    public UploadServiceImpl() throws IngestException {

        if (properties == null) {
            try {
                properties = PropertyUtil.loadProperties(PROPERTIES_CORE, INGEST_MODULE_DIR);
            } catch (final IOException e) {
                VITAM_LOGGER.error(e.getMessage());
                throw new IngestException("properties error");
            }
        }
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }


    /**
     *
     * @param uploadedInputStream
     * @param fileDetail
     * @return
     * @throws IngestException
     */
    @Override
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSipAsStream(@FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail) throws IngestException {

        if (uploadedInputStream == null || fileDetail == null) {
            VITAM_LOGGER.error("input stream null");
            throw new IngestException("error input stream");
        }

        final int tenantId = 0;
        Response response = null;
        final IngestResponse ingestResponse = null;
        String Url = "";
        String containerName = "";
        LogbookClient client = null;
        LogbookParameters parameters = null;

        try {
            VITAM_LOGGER.info("Starting up the save file sip : " + fileDetail.getFileName());

            // guid generated for the container in the workspace
            final GUID guid = GUIDFactory.newOperationIdGUID(tenantId);

            if (guid != null) {
                containerName = guid.getId();
            }
            if (properties != null) {
                VITAM_LOGGER.info(properties.getProperty(URI_WORKSPACE));
                Url = properties.getProperty(URI_WORKSPACE);
            }

            final String eventIdentifier = GUIDFactory.newOperationIdGUID(tenantId).getId(); // Event GUID
            final String eventType = "Process_SIP_unitary"; // Event Type
            final String eventIdentifierProcess = guid.getId(); // Event Identifier Process
            final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.INGEST; // Event Type Process
            final LogbookOutcome outcome = LogbookOutcome.STARTED; // Outcome: status
            final String outcomeDetailMessage = "SIP entry : " + fileDetail.getFileName(); // Outcome detail message
            final String eventIdentifierRequest = guid.getId(); // X-Request-Id
            final String server = properties.getProperty("ingest.core.logbook.server");
            final String sPort = properties.getProperty("ingest.core.logbook.port");
            final int port = Integer.parseInt(sPort);

            // TODO REVIEW Debug not info

            VITAM_LOGGER.info("call journal...");
            // Récupération de la classe paramètre avec ou sans argument
            parameters = buildLogBookParameters(eventIdentifier, eventType, eventIdentifierProcess,
                eventTypeProcess, outcome, outcomeDetailMessage, eventIdentifierRequest);
            client = callLogBookCreate(guid.getId(), tenantId, parameters, server, port);


            parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                "Try to push stream to workspace...");
            VITAM_LOGGER.info("Try to push stream to workspace...");
            runOperation(Url, containerName, uploadedInputStream);
            VITAM_LOGGER.info(" -> push stream to workspace finished");
            parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                "-> push stream to workspace finished");

            parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "Try to call processing...");
            VITAM_LOGGER.info("Try to call processing...");
            final String workflowId = "DefaultIngestWorkflow";
            final String urlProcessing = properties.getProperty(URI_PROCESSING);
            final ProcessingManagementClient processingManagementClient = new ProcessingManagementClient(urlProcessing);
            final String processingRetour = processingManagementClient.executeVitamProcess(containerName, workflowId);
            VITAM_LOGGER.info(" -> process workflow finished " + processingRetour);
            parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "-> process workflow finished");
            client = callLogBookUpdate(client, parameters);

            final UploadResponseDTO uploadResponseDTO = getUploadResponseDTO(fileDetail.getFileName(), 200, "success",
                "201", "success", "200", "success");

            response = Response.ok(uploadResponseDTO, "application/json").build();

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            // TODO test status

        } catch (final ContentAddressableStorageServerException e) {

            if (parameters != null) {
                parameters.setStatus(LogbookOutcome.ERROR);
                parameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    "500_123456"); // 404 = code http, 123456 = code erreur Vitam
                client = callLogBookUpdate(client, parameters);
            }

            VITAM_LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            final UploadResponseDTO uploadResponseDTO =
                getUploadResponseDTO(fileDetail.getFileName(), 500, e.getMessage(),
                    "500", "workspace failed", "500", "error workspace");
            response = Response.ok(uploadResponseDTO, "application/json").build();
        } catch (final Exception e) {

            if (parameters != null) {
                parameters.setStatus(LogbookOutcome.ERROR);
                parameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    "500_123456"); // 404 = code http, 123456 = code erreur Vitam
                // client = callLogBookUpdate(client, parameters);
            }

            VITAM_LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            final UploadResponseDTO uploadResponseDTO =
                getUploadResponseDTO(fileDetail.getFileName(), 500, e.getMessage(),
                    "500", "upload failed", "500", "error ingest");
            // FIXME REVIEW No printStackTrace
            e.printStackTrace();
            response = Response.ok(uploadResponseDTO, "application/json").build();

        } finally {
            if (client != null) {
                client.close();
            }
            // FIXME REVIEW Never ever returns something in finally
            return response;
        }
    }

    /**
     *
     * @param Url
     * @param containerName
     * @param uploadedInputStream
     */
    private void runOperation(String Url, String containerName, InputStream uploadedInputStream)
        throws ContentAddressableStorageServerException {
        // call workspace
        final WorkspaceClient workspaceClient = new WorkspaceClient(Url);
        workspaceClient.unzipSipObject(containerName, uploadedInputStream);
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

        // Available informations
        // Process Id (SIP GUID)
        final String guidSip = "xxx";

        // Changer la configuration du Factory
        // en test
        // LogbookClientFactory.setConfiguration(LogbookClientFactory.LogbookClientType.OPERATIONS, server, port);

        // Récupération du client
        final LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();

        // 2 possibilities
        // 1) Démarrage de l'Opération globale (eventIdentifierProcess) dans INGEST première fois
        client.create(parameters);

        return client;
    }


    /**
     *
     * @param eventIdentifier
     * @param eventType
     * @param eventIdentifierProcess
     * @param eventTypeProcess
     * @param outcome
     * @param outcomeDetailMessage
     * @param eventIdentifierRequest
     * @return
     */
    private LogbookParameters buildLogBookParameters(String eventIdentifier, String eventType,
        String eventIdentifierProcess,
        LogbookTypeProcess eventTypeProcess, LogbookOutcome outcome,
        String outcomeDetailMessage, String eventIdentifierRequest) {
        // Récupération de la classe paramètre avec ou sans argument
        // FIXME REVIEW use correct constructor

        final LogbookParameters parameters =
            LogbookParametersFactory.newLogbookOperationParameters(eventIdentifier,
                eventType, eventIdentifierProcess, eventTypeProcess,
                outcome, outcomeDetailMessage, eventIdentifierRequest);
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
     * Upload service a received SIP from a SIA with a name associated to the SIP
     *
     * @param uploadedInputStream
     * @param fileDetail
     * @param sipName
     * @return
     * @throws IngestException
     */
    @Override
    @POST
    @Path("/uploadName")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSipAsStream(@FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail, @FormParam("name") String sipName)
        throws Exception {
        final Response response = uploadSipAsStream(uploadedInputStream, fileDetail);
        return response;
    }

    /**
     *
     * @param fileName
     * @param httpCode
     * @param message
     * @param vitamCode
     * @param vitamStatus
     * @param engineCode
     * @param engineStatus
     * @return
     */
    private UploadResponseDTO getUploadResponseDTO(String fileName, Integer httpCode, String message, String vitamCode,
        String vitamStatus, String engineCode, String engineStatus) {
        final UploadResponseDTO uploadResponseDTO = new UploadResponseDTO();
        uploadResponseDTO.setFileName(fileName);
        uploadResponseDTO.setHttpCode(httpCode);
        uploadResponseDTO.setMessage(message);
        uploadResponseDTO.setVitamCode(vitamCode);
        uploadResponseDTO.setVitamStatus(vitamStatus);
        uploadResponseDTO.setEngineCode(engineCode);
        uploadResponseDTO.setEngineStatus(engineStatus);
        return uploadResponseDTO;
    }

    @Override
    @GET
    @Path("/status")
    public Response status() {
        return Response.status(200).entity("{\"engine\": \"ingest\", \"status\":\"OK\"}").build();
    }
}
