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
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.DataMigrationBody;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.accession.register.ReferentialAccessionRegisterImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;

import javax.validation.ValidationException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.VitamConfiguration.getTenants;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.status;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class AdminMigrationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminMigrationResource.class);

    private final HashMap<Integer, String> xrequestIds;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    private final AdminManagementConfiguration configuration;
    private final MongoDbAccessAdminImpl mongoAccess;
    private VitamCounterService vitamCounterService;
    private final MetaDataClientFactory metaDataClientFactory;

    AdminMigrationResource(
        AdminManagementConfiguration configuration,
        OntologyLoader ontologyLoader,
        ElasticsearchFunctionalAdminIndexManager indexManager
    ) {
        this(
            LogbookOperationsClientFactory.getInstance(),
            ProcessingManagementClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance(),
            configuration,
            ontologyLoader,
            indexManager
        );
    }

    /**
     * AdminMigrationResource
     *
     * @param logbookOperationsClientFactory logbookOperationsClientFactory
     * @param processingManagementClientFactory processingManagementClientFactory
     * @param workspaceClientFactory workspaceClientFactory
     */
    @VisibleForTesting
    public AdminMigrationResource(
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        AdminManagementConfiguration configuration,
        OntologyLoader ontologyLoader,
        ElasticsearchFunctionalAdminIndexManager indexManager
    ) {
        this.configuration = configuration;
        DbConfigurationImpl adminConfiguration;
        if (configuration.isDbAuthentication()) {
            adminConfiguration = new DbConfigurationImpl(
                configuration.getMongoDbNodes(),
                configuration.getDbName(),
                true,
                configuration.getDbUserName(),
                configuration.getDbPassword()
            );
        } else {
            adminConfiguration = new DbConfigurationImpl(configuration.getMongoDbNodes(), configuration.getDbName());
        }
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        xrequestIds = new HashMap<>();
        mongoAccess = MongoDbAccessAdminFactory.create(adminConfiguration, ontologyLoader, indexManager);
        metaDataClientFactory = MetaDataClientFactory.getInstance();
    }

    /**
     * Migration Api
     *
     * @param headers headers
     * @return Response
     */

    @POST
    @Path("/startMigration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response start(@Context HttpHeaders headers) {
        xrequestIds.clear();

        getTenants().forEach(integer -> xrequestIds.put(integer, GUIDFactory.newGUID().getId()));

        for (Map.Entry<Integer, String> entry : xrequestIds.entrySet()) {
            VitamThreadUtils.getVitamSession().setRequestId(entry.getValue());
            VitamThreadUtils.getVitamSession().setTenantId(entry.getKey());
            migrateTo(entry.getKey());
        }

        return Response.status(Response.Status.ACCEPTED).entity(xrequestIds).build();
    }

    /**
     * Check migration status
     *
     * @return Response
     */
    @HEAD
    @Path("/migrationStatus")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response check() {
        ProcessingManagementClient processingManagementClient = processingManagementClientFactory.getClient();
        if (xrequestIds.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        for (Map.Entry<Integer, String> entry : xrequestIds.entrySet()) {
            VitamThreadUtils.getVitamSession().setTenantId(entry.getKey());
            try {
                ItemStatus operationProcessStatus = processingManagementClient.getOperationProcessStatus(
                    entry.getValue()
                );
                // if one process is on STARTED status return accepted

                boolean isProcessFinished = operationProcessStatus.getGlobalState().equals(ProcessState.COMPLETED);

                // When FATAL occurs, the process state will be set to PAUSE and status to FATAL => To be treated manually
                boolean isProcessPauseFatal =
                    operationProcessStatus.getGlobalState().equals(ProcessState.PAUSE) &&
                    StatusCode.FATAL.equals(operationProcessStatus.getGlobalStatus());

                if (!isProcessFinished && !isProcessPauseFatal) {
                    // At least one workflow is in progress
                    return Response.status(Response.Status.ACCEPTED).build();
                }
            } catch (WorkflowNotFoundException e) {
                LOGGER.warn("Could not find process '" + entry.getValue() + "'. Cleaned Process ?", e);
            } catch (VitamException e) {
                LOGGER.error("Could not check process status " + entry.getValue(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }

        return Response.status(Response.Status.OK).build();
    }

    /**
     * Migrate Collections :
     * To add a new Migration scenary, please add a AUTHORIZED_FIELDS_TO_UPDATE list in the concerned collection to control fields to update.
     * Collection should be controled by its enum family ( FunctionalAdminCollections, OfferCollections, LogbookCollections, ... )
     *
     * @param dataMigrationBody
     * @return
     */
    @Path("collectionMigration")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response migrateCollection(DataMigrationBody dataMigrationBody) {
        // Validate Body
        try {
            validateDataMigrationBody(dataMigrationBody);
        } catch (ValidationException e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, (e.getMessage())))
                .build();
        }

        // Check Collection & fields and start data migration
        if (
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().equals(dataMigrationBody.getCollection()) &&
            authorizedFieldsForCollection(
                AccessionRegisterDetail.AUTHORIZED_FIELDS_TO_UPDATE,
                dataMigrationBody.getFields()
            )
        ) {
            return migrateAccessionRegisterDetails(dataMigrationBody);
        }

        return Response.status(BAD_REQUEST)
            .entity(getErrorEntity(BAD_REQUEST, "Incorrect body fields! Data migration has been aborted !"))
            .build();
    }

    private boolean authorizedFieldsForCollection(List<String> authorizedFieldsToUpdate, List<String> fields) {
        return authorizedFieldsToUpdate.containsAll(fields);
    }

    private Response migrateAccessionRegisterDetails(DataMigrationBody dataMigrationBody) {
        try {
            AccessionRegisterDetailModel accessionRegister = JsonHandler.getFromJsonNode(
                dataMigrationBody.getModel(),
                AccessionRegisterDetailModel.class
            );

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "register ID / Originating Agency: " +
                    accessionRegister.getId() +
                    " / " +
                    accessionRegister.getOriginatingAgency()
                );
            }
            ParametersChecker.checkParameter("Accession Register is a mandatory parameter", accessionRegister);

            if (accessionRegister.getTenant() != null) {
                VitamThreadUtils.getVitamSession().setTenantId(accessionRegister.getTenant());
            } else {
                throw new ValidationException(
                    String.format("The Tenant of the document's ID : %s is not setted !", accessionRegister.getId())
                );
            }

            try (
                ReferentialAccessionRegisterImpl accessionRegisterManagement = new ReferentialAccessionRegisterImpl(
                    mongoAccess,
                    vitamCounterService,
                    metaDataClientFactory,
                    configuration
                )
            ) {
                accessionRegisterManagement.migrateAccessionRegister(accessionRegister, dataMigrationBody.getFields());
                return Response.status(CREATED).build();
            } catch (final BadRequestException e) {
                LOGGER.error(e);
                return Response.status(BAD_REQUEST).entity(getErrorEntity(BAD_REQUEST, e.getMessage())).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage()))
                    .build();
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage()))
                .build();
        }
    }

    private void validateDataMigrationBody(DataMigrationBody dataMigrationBody) throws ValidationException {
        if (StringUtils.isBlank(dataMigrationBody.getCollection())) {
            throw new ValidationException("Collection name is empty !");
        }

        if (dataMigrationBody.getFields() == null || dataMigrationBody.getFields().isEmpty()) {
            throw new ValidationException("Fields are empty !");
        }

        if (JsonHandler.isNullOrEmpty(dataMigrationBody.getModel())) {
            throw new ValidationException("Model is empty !");
        }
    }

    private void migrateTo(Integer tenant) {
        ParametersChecker.checkParameter("TenantId is mandatory", tenant);

        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()
        ) {
            GUID guid = GUIDReader.getGUID(requestId);

            VitamThreadUtils.getVitamSession().setRequestId(guid.getId());

            createOperation(guid);
            workspaceClient.createContainer(guid.getId());

            // No need to backup operation context, this workflow can be re-executed using logbook information
            processingClient.initVitamProcess(guid.getId(), Contexts.DATA_MIGRATION.name());

            RequestResponse<ItemStatus> jsonNodeRequestResponse = processingClient.executeOperationProcess(
                guid.getId(),
                Contexts.DATA_MIGRATION.name(),
                ProcessAction.RESUME.getValue()
            );
            jsonNodeRequestResponse.toResponse();
        } catch (LogbookClientBadRequestException | BadRequestException e) {
            LOGGER.error(e);
            status(BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId).build();
        } catch (
            ContentAddressableStorageServerException
            | VitamClientException
            | InternalServerException
            | InvalidGuidOperationException e
        ) {
            LOGGER.error(e);
            Response.status(INTERNAL_SERVER_ERROR)
                .header(GlobalDataRest.X_REQUEST_ID, requestId)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage()))
                .build();
        }
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }

    private void createOperation(GUID guid) throws LogbookClientBadRequestException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            final LogbookOperationParameters initParameter = LogbookParameterHelper.newLogbookOperationParameters(
                guid,
                Contexts.DATA_MIGRATION.name(),
                guid,
                LogbookTypeProcess.DATA_MIGRATION,
                StatusCode.STARTED,
                VitamLogbookMessages.getLabelOp(
                    String.format("%s.%s", Contexts.DATA_MIGRATION.name(), StatusCode.STARTED.name())
                ) +
                " : " +
                guid,
                guid
            );
            client.create(initParameter);
        } catch (LogbookClientAlreadyExistsException | LogbookClientServerException e) {
            throw new VitamRuntimeException("Internal server error ", e);
        }
    }
}
