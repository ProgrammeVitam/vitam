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
package fr.gouv.vitam.functional.administration.rest;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("/adminmanagement/v1")
@Tag(name = "Functional-Administration")
public class AdminOperationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminOperationResource.class);
    private static final String FUNCTIONAL_ADMINISTRATION_MODULE = "FUNCTIONAL_ADMINISTRATION_MODULE";
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    public AdminOperationResource() {
        this(WorkspaceClientFactory.getInstance(), ProcessingManagementClientFactory.getInstance());
    }

    @VisibleForTesting
    public AdminOperationResource(
        WorkspaceClientFactory workspaceClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory
    ) {
        this.workspaceClientFactory = workspaceClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
    }

    /**
     * Reverts an invalid ingest operation (Purges all units, object groups &amp; binaries, fixes AccessiongRegisters...)
     */
    @Path("invalidIngestCleanup/{ingestOperationId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response startIngestCleanupWorkflow(
        @PathParam("ingestOperationId") String ingestOperationId,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) Integer tenantId
    ) {
        ParametersChecker.checkParameter("Missing tenant", tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();

        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()
        ) {
            workspaceClient.createContainer(operationId);

            createCleanupLogbookOperation(operationId);

            final ProcessingEntry entry = new ProcessingEntry(operationId, Contexts.INGEST_CLEANUP.name());
            entry.getExtraParams().put(WorkerParameterName.ingestOperationIdToCleanup.name(), ingestOperationId);

            // store original query
            workspaceClient.putObject(
                operationId,
                OperationContextMonitor.OperationContextFileName,
                writeToInpustream(OperationContextModel.get(entry))
            );

            // compress file to backup
            OperationContextMonitor.compressInWorkspace(
                workspaceClientFactory,
                operationId,
                Contexts.INGEST_CLEANUP.getLogbookTypeProcess(),
                OperationContextMonitor.OperationContextFileName
            );

            processingClient.initVitamProcess(entry);
            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), operationId);

            LOGGER.info("Cleanup started successfully");

            return Response.status(Response.Status.ACCEPTED)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .entity(new RequestResponseOK<>().setHttpCode(Response.Status.ACCEPTED.getStatusCode()))
                .build();
        } catch (Exception e) {
            LOGGER.error("An error occurred during cleanup", e);
            final Response.Status status = INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .entity(getErrorEntity(status, e.getLocalizedMessage()))
                .build();
        }
    }

    private void createCleanupLogbookOperation(String operationId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException, InvalidGuidOperationException {
        final GUID objectId = GUIDReader.getGUID(operationId);

        try (final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                objectId,
                Contexts.INGEST_CLEANUP.getEventType(),
                objectId,
                LogbookTypeProcess.INTERNAL_OPERATING_OP,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(Contexts.INGEST_CLEANUP.getEventType(), StatusCode.STARTED),
                objectId
            );
            initParameters.putParameterValue(LogbookParameterName.objectIdentifierRequest, operationId);

            logbookClient.create(initParameters);
        }
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String aMessage = StringUtils.isNotEmpty(message)
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(FUNCTIONAL_ADMINISTRATION_MODULE)
            .setState(status.name())
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }
}
