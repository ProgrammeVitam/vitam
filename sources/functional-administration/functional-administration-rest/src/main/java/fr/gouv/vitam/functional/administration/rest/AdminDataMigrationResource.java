/*
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
package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.migration.r7r8.AccessionRegisterMigrationService;
import fr.gouv.vitam.functional.administration.migration.r7r8.MigrationAction;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.status;

/**
 * migrationResource class
 */
@Path("/adminmanagement/v1")
@ApplicationPath("webresources")

public class AdminDataMigrationResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminDataMigrationResource.class);
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private ProcessingManagementClientFactory processingManagementClientFactory;
    private WorkspaceClientFactory workspaceClientFactory;


    private final String ACCESSION_REGISTER_MIGRATION_MIGRATE_URI = "/migration/accessionregister/migrate";
    private final String ACCESSION_REGISTER_MIGRATION_PURGE_URI = "/migration/accessionregister/purge";
    private final String ACCESSION_REGISTER_MIGRATION_STATUS_URI = "/migration/accessionregister/status";


    /**
     * Accession register migration R7 -> R8
     */
    private final AccessionRegisterMigrationService accessionRegisterMigrationService;

    AdminDataMigrationResource(FunctionalBackupService functionalBackupService) {
        this(LogbookOperationsClientFactory.getInstance(), ProcessingManagementClientFactory.getInstance(), WorkspaceClientFactory.getInstance(), new AccessionRegisterMigrationService(functionalBackupService));
    }

    /**
     * Constructor
     *
     * @param logbookOperationsClientFactory    logbookOperationsClientFactory
     * @param processingManagementClientFactory processingManagementClientFactory
     * @param workspaceClientFactory            workspaceClientFactory
     * @param accessionRegisterMigrationService
     */
    @VisibleForTesting
    public AdminDataMigrationResource(
            LogbookOperationsClientFactory logbookOperationsClientFactory,
            ProcessingManagementClientFactory processingManagementClientFactory,
            WorkspaceClientFactory workspaceClientFactory,
            AccessionRegisterMigrationService accessionRegisterMigrationService) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.accessionRegisterMigrationService = accessionRegisterMigrationService;
    }


    /**
     * Migration Api
     *
     * @param headers headers
     * @return Response
     */

    @POST
    @Path("/migrate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response migrateTo(@Context HttpHeaders headers) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        ParametersChecker.checkParameter("TenantId is mandatory", xTenantId);

        int tenant = Integer.parseInt(xTenantId);
        return migrateTo(tenant);
    }

    public Response migrateTo(Integer tenant) {
        ParametersChecker.checkParameter("TenantId is mandatory", tenant);

        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        VitamThreadUtils.getVitamSession().setTenantId(tenant);

        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
             WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            GUID guid = GUIDReader.getGUID(requestId);

            VitamThreadUtils.getVitamSession().setRequestId(guid.getId());

            createOperation(guid);
            workspaceClient.createContainer(guid.getId());

            processingClient.initVitamProcess(guid.getId(), Contexts.DATA_MIGRATION.name());

            RequestResponse<JsonNode> jsonNodeRequestResponse =
                    processingClient.executeOperationProcess(guid.getId(), Contexts.DATA_MIGRATION.name(), ProcessAction.RESUME.getValue());
            return jsonNodeRequestResponse.toResponse();

        } catch (LogbookClientBadRequestException | BadRequestException e) {
            LOGGER.error(e);
            return status(BAD_REQUEST).build();

        } catch (ContentAddressableStorageServerException | ContentAddressableStorageAlreadyExistException | VitamClientException | InternalServerException | InvalidGuidOperationException e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }
    }


    /**
     * API for Accession Register migration
     *
     * @return the response
     */
    @Path(ACCESSION_REGISTER_MIGRATION_MIGRATE_URI)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response startAccessionRegisterMigration() {
        return handleMigrationProcess(MigrationAction.MIGRATE);
    }

    /**
     * API for Accession Register migration
     *
     * @return the response
     */
    @Path(ACCESSION_REGISTER_MIGRATION_PURGE_URI)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response startAccessionRegisterMigrationPurge() {
        return handleMigrationProcess(MigrationAction.PURGE);
    }

    private Response handleMigrationProcess(MigrationAction migrationAction) {
        try {

            VitamThreadUtils.getVitamSession().initIfAbsent(VitamConfiguration.getAdminTenant());

            boolean started = this.accessionRegisterMigrationService.tryStartMigration(migrationAction);

            if (started) {
                LOGGER.info("Accession Register migration started successfully");
                return Response.accepted(new ResponseMessage("OK")).build();
            } else {
                LOGGER.warn("Accession Register migration already in progress");
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ResponseMessage("Accession Register migration already in progress")).build();
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred during Accession Register migration", e);
            return Response.serverError().entity(new ResponseMessage(e.getMessage())).build();
        }
    }
    /**
     * API for Accession Register migration status check
     *
     * @return the response
     */
    @Path(ACCESSION_REGISTER_MIGRATION_STATUS_URI)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response isAccessionRegisterMigrationInProgress() {

        try {
            boolean started = this.accessionRegisterMigrationService.isMigrationInProgress();

            if (started) {
                LOGGER.info("Accession Register migration still in progress");
                return Response.ok(new ResponseMessage("Accession Register migration in progress")).build();
            } else {
                LOGGER.info("No active migration");
                return Response.status(Response.Status.NOT_FOUND).entity(new ResponseMessage("No active migration"))
                        .build();
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred during Accession Register migration", e);
            return Response.serverError().entity(new ResponseMessage(e.getMessage())).build();
        }
    }

    public static class ResponseMessage {

        private String message;

        public ResponseMessage(String message) {
            this.message = message;
        }

        public ResponseMessage() {
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String aMessage =
                (message != null && !message.trim().isEmpty()) ? message
                        : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }


    private void createOperation(GUID guid)
            throws LogbookClientBadRequestException {

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

            final LogbookOperationParameters initParameter =
                    LogbookParametersFactory.newLogbookOperationParameters(
                            guid,
                            "DATA_MIGRATION",
                            guid,
                            LogbookTypeProcess.DATA_MIGRATION,
                            StatusCode.STARTED,
                            VitamLogbookMessages.getLabelOp("DATA_MIGRATION.STARTED") + " : " + guid,
                            guid);
            client.create(initParameter);
        } catch (LogbookClientAlreadyExistsException | LogbookClientServerException e) {
            throw new VitamRuntimeException("Internal server error ", e);
        }
    }


}
