/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.functional.administration.rest;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
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
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
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

import java.util.List;


/**
 * resource
 */
@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
public class ProbativeValueResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeValueResource.class);
    private static final String EXPORT_PROBATIVE_VALUE = "EXPORT_PROBATIVE_VALUE";
    private static final String BAD_REQUEST_EXCEPTION = "Bad request Exception ";

    /**
     * probative value  service
     */
    private ProcessingManagementClientFactory processingManagementClientFactory =
        ProcessingManagementClientFactory.getInstance();
    private LogbookOperationsClientFactory logbookOperationsClientFactory =
        LogbookOperationsClientFactory.getInstance();

    private WorkspaceClientFactory workspaceClientFactory = WorkspaceClientFactory.getInstance();

    @VisibleForTesting
    public ProbativeValueResource(
        ProcessingManagementClientFactory processingManagementClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        WorkspaceClientFactory workspaceClientFactory) {
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }


    ProbativeValueResource() {  /*nothing to do   */}

    private void createProbativeOperation(String operationId)
        throws
        LogbookClientServerException, InvalidGuidOperationException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

            final LogbookOperationParameters initParameters =
                LogbookParametersFactory.newLogbookOperationParameters(
                    GUIDReader.getGUID(operationId),
                    "EXPORT_PROBATIVE_VALUE",
                    GUIDReader.getGUID(operationId),
                    LogbookTypeProcess.AUDIT,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getLabelOp("EXPORT_PROBATIVE_VALUE.STARTED") + " : " +
                        GUIDReader.getGUID(operationId),
                    GUIDReader.getGUID(operationId));

            client.create(initParameters);

        }
    }

    @POST
    @Path("/probativevalueexport")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportProbativeValue(ProbativeValueRequest probativeValueRequest) {

        Response.Status status;
        LOGGER.debug("DEBUG: start selectUnits {}", probativeValueRequest.getDslQuery());
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        try {
            checkEmptyQuery(probativeValueRequest.getDslQuery());

            checkUsageNotEmptyOrNotBinaryMaster(probativeValueRequest.getUsages());

            try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
                WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

                workspaceClient.createContainer(operationId);

                createProbativeOperation(operationId);

                workspaceClient.putObject(operationId, "request", JsonHandler.writeToInpustream(probativeValueRequest));

                processingClient.initVitamProcess(Contexts.EXPORT_PROBATIVE_VALUE.name(), operationId,
                    EXPORT_PROBATIVE_VALUE);

                RequestResponse<JsonNode> jsonNodeRequestResponse =
                    processingClient.executeOperationProcess(operationId, EXPORT_PROBATIVE_VALUE,
                        Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
                return jsonNodeRequestResponse.toResponse();

            } catch (ContentAddressableStorageServerException | ContentAddressableStorageAlreadyExistException |

                VitamClientException | LogbookClientServerException | InternalServerException | InvalidGuidOperationException e) {
                LOGGER.error("Error while exporting probative value", e);

                return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();

            } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException e) {
                return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
            }

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Response.Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error("Empty query is impossible", e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY);
        }

    }


    private Response buildErrorResponse(VitamCode vitamCode) {
        return Response.status(vitamCode.getStatus())
            .entity(new RequestResponseError().setError(new VitamError(VitamCodeHelper.getCode(vitamCode))
                .setContext(vitamCode.getService().getName()).setState(vitamCode.getDomain().getName())
                .setMessage(vitamCode.getMessage()).setDescription(vitamCode.getMessage())).toString())
            .build();
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }



    private void checkEmptyQuery(JsonNode queryDsl)
        throws InvalidParseOperationException, BadRequestException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl.deepCopy());
        if (parser.getRequest().getNbQueries() == 0 && parser.getRequest().getRoots().isEmpty()) {
            throw new BadRequestException("Query cannot be empty");
        }
    }

    private void checkUsageNotEmptyOrNotBinaryMaster(List<String> usages) throws BadRequestException {

        if (usages.isEmpty()) {
            throw new BadRequestException("Query cannot be empty");
        }
        for (String usage : usages) {
            if (usage.equals("BinaryMaster")) {
                return;
            }
        }
        throw new BadRequestException("BinaryMaster has to be on the usage list");
    }
}
