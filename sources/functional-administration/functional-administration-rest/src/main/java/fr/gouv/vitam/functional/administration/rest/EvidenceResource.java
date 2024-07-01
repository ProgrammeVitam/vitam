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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.contract.AccessContractImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * Lifecycle traceability audit resource
 */
@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class EvidenceResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceResource.class);
    private static final String BAD_REQUEST_EXCEPTION = "Bad request Exception ";
    private static final String OPERATION_ID_MANDATORY = "Operation id Mandatory";
    private static final String ACCESS_CONTRACT = "AccessContract";

    /**
     * Evidence service
     */
    private ProcessingManagementClientFactory processingManagementClientFactory =
        ProcessingManagementClientFactory.getInstance();
    private LogbookOperationsClientFactory logbookOperationsClientFactory =
        LogbookOperationsClientFactory.getInstance();

    private WorkspaceClientFactory workspaceClientFactory = WorkspaceClientFactory.getInstance();

    private final MongoDbAccessAdminImpl mongoDbAccess;
    private final VitamCounterService vitamCounterService;

    @VisibleForTesting
    EvidenceResource(
        ProcessingManagementClientFactory processingManagementClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        MongoDbAccessAdminImpl mongoDbAccess,
        VitamCounterService vitamCounterService
    ) {
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.mongoDbAccess = mongoDbAccess;
        this.vitamCounterService = vitamCounterService;
    }

    EvidenceResource(MongoDbAccessAdminImpl mongoDbAccess, VitamCounterService vitamCounterService) {
        this.mongoDbAccess = mongoDbAccess;
        this.vitamCounterService = vitamCounterService;
    }

    private void createEvidenceAuditOperation(String operationId, AccessContractModel accessContract)
        throws LogbookClientServerException, InvalidGuidOperationException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDReader.getGUID(operationId),
                "EVIDENCE_AUDIT",
                GUIDReader.getGUID(operationId),
                LogbookTypeProcess.AUDIT,
                StatusCode.STARTED,
                VitamLogbookMessages.getLabelOp("EVIDENCE_AUDIT.STARTED") + " : " + GUIDReader.getGUID(operationId),
                GUIDReader.getGUID(operationId)
            );

            // Add access contract rights
            ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
            rightsStatementIdentifier.put(ACCESS_CONTRACT, accessContract.getIdentifier());
            initParameters.putParameterValue(
                LogbookParameterName.rightsStatementIdentifier,
                rightsStatementIdentifier.toString()
            );

            client.create(initParameters);
        }
    }

    @POST
    @Path("/evidenceaudit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response audit(JsonNode queryDsl) {
        Response.Status status;
        LOGGER.debug("DEBUG: start selectUnits {}", queryDsl);
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient();
            AccessContractImpl accessContractService = new AccessContractImpl(mongoDbAccess, vitamCounterService)
        ) {
            checkEmptyQuery(queryDsl);

            AccessContractModel contract = accessContractService.findByIdentifier(
                VitamThreadUtils.getVitamSession().getContractId()
            );
            if (contract == null) {
                throw new AccessUnauthorizedException("Contract Not Found");
            }

            JsonNode finalQuery = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
                queryDsl,
                contract
            );

            workspaceClient.createContainer(operationId);

            createEvidenceAuditOperation(operationId, contract);

            ObjectNode options = JsonHandler.createObjectNode().put("correctiveOption", false);
            workspaceClient.putObject(operationId, "evidenceOptions", JsonHandler.writeToInpustream(options));

            workspaceClient.putObject(operationId, "query.json", JsonHandler.writeToInpustream(finalQuery));

            // No need to backup operation context, this workflow can be re-executed multiple times.
            processingClient.initVitamProcess(operationId, Contexts.EVIDENCE_AUDIT.name());

            RequestResponse<ItemStatus> jsonNodeRequestResponse = processingClient.executeOperationProcess(
                operationId,
                Contexts.EVIDENCE_AUDIT.name(),
                ProcessAction.RESUME.getValue()
            );
            return jsonNodeRequestResponse.toResponse();
        } catch (
            ContentAddressableStorageServerException
            | VitamClientException
            | InternalServerException
            | InvalidGuidOperationException
            | ReferentialException e
        ) {
            LOGGER.error("Error while auditing", e);

            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage()))
                .build();
        } catch (
            LogbookClientServerException | LogbookClientBadRequestException | LogbookClientAlreadyExistsException e
        ) {
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Response.Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error("Empty query is impossible", e);
            return buildErrorResponse();
        }
    }

    @POST
    @Path("rectificationaudit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rectificationAudit(String operation) {
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient();
            AccessContractImpl accessContractService = new AccessContractImpl(mongoDbAccess, vitamCounterService)
        ) {
            ParametersChecker.checkParameter(OPERATION_ID_MANDATORY, operation);
            workspaceClient.createContainer(operationId);
            ObjectNode option = JsonHandler.createObjectNode()
                .put("operation", operation)
                .put("correctiveOption", true);

            workspaceClient.putObject(operationId, "evidenceOptions", JsonHandler.writeToInpustream(option));

            AccessContractModel contract = accessContractService.findByIdentifier(
                VitamThreadUtils.getVitamSession().getContractId()
            );
            if (contract == null) {
                throw new AccessUnauthorizedException("Contract Not Found");
            }

            // FIXME: 01/01/2020 (new operation should be created) operation id concern audit workflow that save rapport in offer. rectification shoud be an other operation that use an operation of audit to get rapport
            createRectificationAuditOperation(operationId, contract);

            // No need to backup operation context, this workflow can be re-executed multiple times.
            processingClient.initVitamProcess(operationId, Contexts.RECTIFICATION_AUDIT.name());

            RequestResponse<ItemStatus> jsonNodeRequestResponse = processingClient.executeOperationProcess(
                operationId,
                Contexts.RECTIFICATION_AUDIT.name(),
                ProcessAction.RESUME.getValue()
            );
            return jsonNodeRequestResponse.toResponse();
        } catch (
            ContentAddressableStorageServerException
            | InvalidParseOperationException
            | LogbookClientException
            | VitamClientException
            | InternalServerException
            | InvalidGuidOperationException
            | ReferentialException e
        ) {
            LOGGER.error("Error while auditing", e);

            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage()))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error("Empty query is impossible", e);
            return buildErrorResponse();
        }
    }

    private void createRectificationAuditOperation(String operationId, AccessContractModel accessContract)
        throws LogbookClientServerException, InvalidGuidOperationException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDReader.getGUID(operationId),
                Contexts.RECTIFICATION_AUDIT.getEventType(),
                GUIDReader.getGUID(operationId),
                LogbookTypeProcess.AUDIT,
                StatusCode.STARTED,
                VitamLogbookMessages.getLabelOp("RECTIFICATION_AUDIT.STARTED") +
                " : " +
                GUIDReader.getGUID(operationId),
                GUIDReader.getGUID(operationId)
            );

            // Add access contract rights
            ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
            rightsStatementIdentifier.put(ACCESS_CONTRACT, accessContract.getIdentifier());
            initParameters.putParameterValue(
                LogbookParameterName.rightsStatementIdentifier,
                rightsStatementIdentifier.toString()
            );

            client.create(initParameters);
        }
    }

    private Response buildErrorResponse() {
        VitamCode vitamCode = VitamCode.GLOBAL_EMPTY_QUERY;
        return Response.status(vitamCode.getStatus())
            .entity(
                new RequestResponseError()
                    .setError(
                        new VitamError(VitamCodeHelper.getCode(vitamCode))
                            .setContext(vitamCode.getService().getName())
                            .setState(vitamCode.getDomain().getName())
                            .setMessage(vitamCode.getMessage())
                            .setDescription(vitamCode.getMessage())
                    )
                    .toString()
            )
            .build();
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String reasonPhrase = status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name();
        String aMessage = (message != null && !message.trim().isEmpty()) ? message : reasonPhrase;
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }

    private void checkEmptyQuery(JsonNode queryDsl) throws InvalidParseOperationException, BadRequestException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl.deepCopy());
        if (parser.getRequest().getNbQueries() == 0 && parser.getRequest().getRoots().isEmpty()) {
            throw new BadRequestException("Query cant be empty");
        }
    }
}
