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
package fr.gouv.vitam.ingest.internal.upload.rest;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.internal.api.upload.UploadService;
import fr.gouv.vitam.ingest.internal.common.util.LogbookOperationParametersList;
import fr.gouv.vitam.ingest.internal.model.UploadResponseDTO;
import fr.gouv.vitam.ingest.internal.upload.core.UploadSipHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
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
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * IngestInternalResource implements UploadService
 *
 */
@Path("/ingest/v1")
public class IngestInternalResource implements UploadService {

    private static VitamLogger VITAM_LOGGER = VitamLoggerFactory.getInstance(IngestInternalResource.class);

    private static final String FOLDER_SIP = "SIP";

    private IngestInternalConfiguration configuration;
    private LogbookParameters parameters;
    private LogbookClient logbookClient;
    private ProcessingManagementClient processingClient;
    private WorkspaceClient workspaceClient;

    /**
     * IngestInternalResource constructor
     *
     */
    public IngestInternalResource(IngestInternalConfiguration configuration) {
        this.configuration=configuration;
    }

    /**
     *
     * Get IngestInternalServer Status
     * @return status
     */
    @Override
    @GET
    @Path("/status")
    @Consumes("application/json")
    @Produces("application/json")
    public Response status() {
        return Response.status(200).entity("").build();
    }

    /**
     *
     * @param uploadedInputStream
     * @param fileDetail
     * @return
     */
    @Override
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSipAsStream(@FormDataParam("part") List<FormDataBodyPart> partList) {

        Response response = null;
        try {

            ParametersChecker.checkParameter("partList is a Mandatory parameter", partList);

            LogbookOperationParametersList logbookOperationParametersList= partList.get(0).getValueAs(LogbookOperationParametersList.class);

            ParametersChecker.checkParameter("logbookOperationParametersList is a Mandatory parameter", logbookOperationParametersList);
            int tenantId = 0; // default tenanId
            // guid for the container in the workspace
            final GUID containerGUID = GUIDFactory.newGUID();
            final GUID ingestGuid = GUIDReader.getGUID(logbookOperationParametersList.getLogbookOperationList().get(0).getMapParameters().get(LogbookParameterName.eventIdentifier));

            logbookClient = logbookInitialisation(ingestGuid, containerGUID, tenantId);

            // Log Ingest External operations
            VITAM_LOGGER.info("Log Ingest External operations");

            for(LogbookParameters logbookParameters: logbookOperationParametersList.getLogbookOperationList() ){
                logbookClient.update(logbookParameters);
            }

            InputStream uploadedInputStream=null;

            if (partList.size()==2) {
                uploadedInputStream= partList.get(1).getValueAs(InputStream.class);


                ParametersChecker.checkParameter("HTTP Request must contains 2 multiparts part", uploadedInputStream);

                // Save sip file
                VITAM_LOGGER.info("Starting up the save file sip");
                // workspace
                pushSipStreamToWorkspace(configuration.getWorkspaceUrl(), containerGUID.getId(), uploadedInputStream, parameters);

                // processing
                logbookClient = callProcessingEngine(parameters, logbookClient, containerGUID.getId());
            } else {
                callLogbookUpdate(logbookClient, parameters, LogbookOutcome.ERROR, "Update logbook");
            }
            final UploadResponseDTO uploadResponseDTO =
                UploadSipHelper.getUploadResponseDTO("Sip file", 200, "success",
                    "201", "success", "200", "success");
            response = Response.ok(uploadResponseDTO, "application/json").build();

        } catch (final ContentAddressableStorageException e) {

            if (parameters != null) {
                try {
                    logbookClient = callLogbookUpdate(logbookClient, parameters, LogbookOutcome.ERROR,"error workspace");
                } catch (final LogbookClientException e1) {
                    VITAM_LOGGER.error(e1.getMessage(), e1);
                }
            }

            VITAM_LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            final UploadResponseDTO uploadResponseDTO =
                UploadSipHelper.getUploadResponseDTO("Sip file", 500, e.getMessage(),
                    "500", "workspace failed", "500", "error workspace");
            response = Response.ok(uploadResponseDTO, "application/json").build();
        } catch (final  InvalidGuidOperationException | InvalidParseOperationException | ProcessingException | LogbookClientException  e) {

            if (parameters != null) {
                try {
                    logbookClient = callLogbookUpdate(logbookClient, parameters, LogbookOutcome.ERROR, "error ingest");
                } catch (final LogbookClientException e1) {
                    VITAM_LOGGER.error(e1.getMessage(), e1);
                }
            }

            VITAM_LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            final UploadResponseDTO uploadResponseDTO =
                UploadSipHelper.getUploadResponseDTO("Sip file", 500, e.getMessage(),
                    "500", "upload failed", "500", "error ingest");
            response = Response.ok(uploadResponseDTO, "application/json").build();

        } 
        return response;
    }


    private LogbookClient logbookInitialisation(final GUID ingestGuid, final GUID containerGUID, int tenantId)
        throws LogbookClientNotFoundException,
        LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException {

        final String eventType="Process_SIP_unitary";

        parameters = LogbookParametersFactory.newLogbookOperationParameters(
            ingestGuid, eventType, containerGUID,
            LogbookTypeProcess.INGEST, LogbookOutcome.STARTED,
            ingestGuid != null ? ingestGuid.toString() : "outcomeDetailMessage",
                ingestGuid);

        VITAM_LOGGER.debug("call journal...");
        final LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
        client.create(parameters);

        return client;

    }

    private LogbookClient callLogbookUpdate(LogbookClient client, LogbookParameters parameters, LogbookOutcome logbookOutcome, String outcomeDetailMessage)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {

        parameters.setStatus(logbookOutcome);
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage);
        client.update(parameters);

        return client;
    }

    /**
     *
     * @param urlWorkspace
     * @param containerName
     * @param uploadedInputStream
     * @throws ContentAddressableStorageServerException 
     */
    private void pushSipStreamToWorkspace(final String urlWorkspace, final String containerName,
        final InputStream uploadedInputStream, final LogbookParameters parameters)
            throws ContentAddressableStorageNotFoundException, ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {

        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "Try to push stream to workspace...");
        VITAM_LOGGER.info("Try to push stream to workspace...");

        // call workspace
        if (workspaceClient == null) {
            workspaceClient = WorkspaceClientFactory.create(urlWorkspace);
        }
        if (!workspaceClient.isExistingContainer(containerName)) {
            workspaceClient.createContainer(containerName);
            workspaceClient.unzipObject(containerName, FOLDER_SIP, uploadedInputStream);
        }else {
            throw new ContentAddressableStorageAlreadyExistException( containerName + "already exist");
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

        processingClient =   ProcessingManagementClientFactory.create(configuration.getProcessingUrl());
        final String processingRetour = processingClient.executeVitamProcess(containerName, workflowId);
        VITAM_LOGGER.info(" -> process workflow finished " + processingRetour);
        return callLogbookUpdate(client, parameters, LogbookOutcome.OK, "process workflow finished");
    }
}
