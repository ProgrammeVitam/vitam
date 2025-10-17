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

package fr.gouv.vitam.collect.internal.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateResult;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.dto.UploadSipResult;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalNotFoundException;
import fr.gouv.vitam.collect.common.exception.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.service.BulkAtomicUpdateMetadataService;
import fr.gouv.vitam.collect.internal.core.service.MetadataService;
import fr.gouv.vitam.collect.internal.core.service.SipService;
import fr.gouv.vitam.collect.internal.core.service.TransactionService;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.elimination.DeletionRequestBody;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.utils.BulkAtomicUpdateModelUtils;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.CommonMediaType.TEXT_CSV;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.Response.Status.ACCEPTED;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;

@Path("/collect-internal/v1/transactions")
public class TransactionInternalResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionInternalResource.class);
    private static final String EMPTY_QUERY_IS_IMPOSSIBLE = "Empty query is impossible";
    private static final String INVALID_QUERY_DSL_EXCEPTION = "Invalid query DSL ";
    private static final String EXECUTION_OF_DSL_VITAM_FROM_COLLECT_ONGOING =
        "Execution of DSL Vitam from Collect ongoing...";
    private static final String DEBUG = "DEBUG {}";
    private final TransactionService transactionService;
    private final MetadataService metadataService;
    private final SipService sipService;
    private final BulkAtomicUpdateMetadataService bulkAtomicUpdateMetadataService;

    public TransactionInternalResource(
        TransactionService transactionService,
        SipService sipService,
        MetadataService metadataService,
        BulkAtomicUpdateMetadataService bulkAtomicUpdateMetadataService
    ) {
        this.transactionService = transactionService;
        this.sipService = sipService;
        this.metadataService = metadataService;
        this.bulkAtomicUpdateMetadataService = bulkAtomicUpdateMetadataService;
    }

    @GET
    @Path("/withAutomaticIngest")
    @Produces(APPLICATION_JSON)
    public Response getTransactionsToAutomaticallyIngest() throws CollectInternalException {
        return CollectRequestResponse.toResponseOK(
            transactionService
                .findValidatedAutoIngestTransactions()
                .stream()
                .map(CollectHelper::convertTransactionModelToTransactionDto)
                .collect(Collectors.toList())
        );
    }

    @Path("/{transactionId}")
    @GET
    @Produces(APPLICATION_JSON)
    public Response getTransactionById(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);

            TransactionModel transactionModel = getTransaction(transactionId);

            TransactionDto transactionDto = CollectHelper.convertTransactionModelToTransactionDto(transactionModel);

            return CollectRequestResponse.toResponseOK(transactionDto);
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("Error when get transaction by Id. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("Error when get transaction by Id. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error when get transaction by Id. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTransaction(TransactionDto transactionDto) {
        try {
            ParametersChecker.checkParameter("You must supply transaction data!", transactionDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(transactionDto));
            TransactionModel transactionModel = transactionService.replaceTransaction(transactionDto);
            TransactionDto result = CollectHelper.convertTransactionModelToTransactionDto(transactionModel);
            return CollectRequestResponse.toResponseOK(result);
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("Error updating transaction. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("Error updating transaction. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error updating transaction. Internal Server Error :", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}")
    @DELETE
    @Produces(APPLICATION_JSON)
    public Response deleteTransactionById(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);

            TransactionModel transactionModel = getTransaction(transactionId);

            // FIXME : Need to check transaction status !

            transactionService.deleteTransaction(transactionModel.getId());
            return Response.status(Response.Status.OK).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("Error while deleting transaction by Id. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("Error while deleting transaction by Id. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error while deleting transaction by Id. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/units")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response uploadArchiveUnit(@PathParam("transactionId") String transactionId, JsonNode unitJsonNode) {
        try {
            SanityChecker.checkParameter(transactionId);
            SanityChecker.checkJsonAll(unitJsonNode);

            TransactionModel transactionModel = getTransaction(transactionId);
            transactionService.ensureTransactionIsOpen(transactionModel);
            JsonNode savedUnitJsonNode = metadataService.saveArchiveUnit(unitJsonNode, transactionModel);

            return CollectRequestResponse.toResponseOK(savedUnitJsonNode);
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("Error while uploading archive unit. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException e) {
            LOGGER.error("Error while uploading archive unit. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error while uploading archive unit. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    /**
     * select Unit
     *
     * @param jsonQuery as String { $query : query}
     */
    @Path("/{transactionId}/units")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response selectUnits(@PathParam("transactionId") String transactionId, JsonNode jsonQuery) {
        try {
            // FIXME : Check transaction existence / status?

            final RequestResponseOK<JsonNode> units = metadataService.selectUnitsByTransactionId(
                jsonQuery,
                transactionId
            );
            return Response.status(Response.Status.OK).entity(units).build();
        } catch (Exception e) {
            LOGGER.error("Error when getting units in metadata :", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response closeTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            TransactionModel transaction = getTransaction(transactionId);

            transactionService.closeTransaction(transaction);
            sipService.generateSipAsync(transaction);

            return Response.status(OK).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("An error occurred while closing transaction. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("An error occurred while closing transaction. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred while closing transaction. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/abort")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response abortTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            transactionService.abortTransaction(transactionId);
            sipService.cleanupSip(transactionId);
            return Response.status(OK).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("An error occurred while aborting transaction. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("An error occurred while aborting transaction. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred while aborting transaction. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/reopen")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response reopenTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            transactionService.reopenTransaction(transactionId);
            sipService.cleanupSip(transactionId);
            return Response.status(OK).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("An error occurred while reopening transaction. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("An error occurred while reopening transaction. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred while reopening transaction. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/awaitTransactionValidation")
    @POST
    @Produces(APPLICATION_JSON)
    public Response awaitTransactionValidation(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            transactionService.awaitTransactionValidationForIngest(transactionId);
            return Response.status(OK).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("Error while awaiting transaction validation. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("Error while awaiting transaction validation. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error while awaiting transaction validation. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/downloadSIP")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadSip(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            TransactionModel transaction = getTransaction(transactionId);

            transactionService.checkSipDownloadable(transaction);

            InputStream sipInputStream = sipService.getIngestedFileFromWorkspace(transactionId);
            return Response.ok(sipInputStream).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("Error while downloading transaction SIP. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("Error while downloading transaction SIP. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error while downloading transaction SIP. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/units/metadata/csv")
    @PUT
    @Consumes(TEXT_CSV)
    @Produces(APPLICATION_JSON)
    public Response updateUnitsWithMetadataCsv(
        @PathParam("transactionId") String transactionId,
        InputStream metadataCsvInputStream
    ) {
        try {
            ParametersChecker.checkParameter("DOCUMENT_IS_MANDATORY", metadataCsvInputStream);
            SanityChecker.checkParameter(transactionId);

            TransactionModel transaction = getTransaction(transactionId);
            transactionService.ensureTransactionIsOpen(transaction);

            final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
            File file = PropertiesUtils.fileFromTmpFolder(String.format("metadata_%s.csv", requestId));

            // Check Html Pattern
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                StreamUtils.copy(metadataCsvInputStream, fileOutputStream);
                if (file.length() == 0) {
                    throw new CollectInternalInvalidRequestException("Empty file");
                }
                SanityChecker.checkHTMLFile(file);

                try (InputStream sanityStream = new FileInputStream(file)) {
                    metadataService.updateUnitsWithMetadataCsv(transaction, sanityStream);
                }
            } finally {
                FileUtils.deleteQuietly(file);
            }
            return Response.ok(new RequestResponseOK<>()).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("An occurred while updating metadata. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | CollectInternalInvalidRequestException e) {
            LOGGER.error("An occurred while updating metadata. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("An occurred while updating metadata. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/units/metadata/jsonl")
    @PUT
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    public Response updateUnitsWithMetadataJsonl(
        @PathParam("transactionId") String transactionId,
        InputStream metadataJsonlInputStream
    ) {
        try {
            ParametersChecker.checkParameter("DOCUMENT_IS_MANDATORY", metadataJsonlInputStream);
            SanityChecker.checkParameter(transactionId);

            TransactionModel transaction = getTransaction(transactionId);

            transactionService.ensureTransactionIsOpen(transaction);

            metadataService.updateUnitsWithJsonlMetadata(transaction, metadataJsonlInputStream);

            return Response.ok(new RequestResponseOK<>()).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("An error occurred while updating metadata. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | CollectInternalInvalidRequestException e) {
            LOGGER.error("An occurred while updating metadata. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("An occurred while updating metadata. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/upload")
    @POST
    @Consumes({ CommonMediaType.ZIP })
    @Produces(APPLICATION_JSON)
    public Response uploadTransactionZip(
        @PathParam("transactionId") String transactionId,
        InputStream inputStreamObject,
        @HeaderParam(GlobalDataRest.X_ENCODING) @Nullable String encoding,
        @HeaderParam(GlobalDataRest.X_ATTACHEMENT_ID) @Nullable String attachementId
    ) {
        try {
            ParametersChecker.checkParameter("You must supply a file!", inputStreamObject);
            SanityChecker.checkParameter(transactionId);

            TransactionModel transactionModel = getTransaction(transactionId);

            transactionService.ensureTransactionIsOpen(transactionModel);

            return transactionService.uploadTransactionZip(
                inputStreamObject,
                transactionModel,
                encoding,
                attachementId
            );
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("An error occurred while uploading the ZIP. Bad Request", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("An error occurred while uploading the ZIP. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred while uploading the ZIP. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/status/{transactionStatus}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response changeTransactionStatus(
        @PathParam("transactionId") String transactionId,
        @PathParam("transactionStatus") TransactionStatus transactionStatus
    ) {
        try {
            SanityChecker.checkParameter(transactionId);
            TransactionModel transaction = getTransaction(transactionId);
            transactionService.changeTransactionStatus(transactionStatus, transaction);
            return Response.status(OK).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("An error occurred while updating status. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("An error occurred while updating status. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred while updating status. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/operation-id/{operationId}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response attachVitamOperationId(
        @PathParam("transactionId") String transactionId,
        @PathParam("operationId") String operationId
    ) {
        try {
            SanityChecker.checkParameter(transactionId);
            SanityChecker.checkParameter(operationId);
            transactionService.attachVitamOperationId(transactionId, operationId);
            return Response.status(OK).build();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("An error occurred while transaction with ingest operation id. Not Found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (CollectInternalInvalidRequestException | InvalidParseOperationException e) {
            LOGGER.error("An error occurred while transaction with ingest operation id. Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred while transaction with ingest operation id. Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    private void checkEmptyQuery(JsonNode queryDsl) throws InvalidParseOperationException, BadRequestException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl.deepCopy());
        if (parser.getRequest().getNbQueries() == 0 && parser.getRequest().getRoots().isEmpty()) {
            throw new BadRequestException("Query cant be empty");
        }
    }

    /**
     * Select units with inherited rules
     *
     * @param transactionId as transaction Id
     * @param queryDsl as JsonNode
     * @return an archive unit result list with inherited rules
     */
    @GET
    @Path("/{transactionId}/unitsWithInheritedRules")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectUnitsWithInheritedRules(@PathParam("transactionId") String transactionId, JsonNode queryDsl) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_COLLECT_ONGOING);
        Response.Status status;
        JsonNode result;
        LOGGER.debug("DEBUG: start selectUnitsWithInheritedRules {}", queryDsl);
        try {
            SanityChecker.checkJsonAll(queryDsl);
            checkEmptyQuery(queryDsl);

            // FIXME : ensure transaction exists

            result = metadataService.selectUnitsWithInheritedRules(transactionId, queryDsl);
            LOGGER.debug(DEBUG, result);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(INVALID_QUERY_DSL_EXCEPTION, e);
            return CollectRequestResponse.toVitamError(Response.Status.BAD_REQUEST, EMPTY_QUERY_IS_IMPOSSIBLE);
        } catch (BadRequestException e) {
            LOGGER.error(EMPTY_QUERY_IS_IMPOSSIBLE, e);
            return CollectRequestResponse.toVitamError(
                VitamCode.GLOBAL_EMPTY_QUERY.getStatus(),
                EMPTY_QUERY_IS_IMPOSSIBLE
            );
        } catch (final Exception ve) {
            LOGGER.error(ve);
            status = Response.Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(
                    new VitamError<JsonNode>(status.name())
                        .setHttpCode(status.getStatusCode())
                        .setMessage(ve.getMessage())
                        .setDescription(status.getReasonPhrase())
                )
                .build();
        }
        return Response.status(Response.Status.OK).entity(result).build();
    }

    /**
     * Bulk atomic update of archive units with json queries of the provided collect transaction.
     * <br />
     * Units are update in blocking mode (might take a few moments to proceed before returning).
     * Please ensure proper request size / timeout is configured.
     *
     * @param updateQueriesJson the bulk update queries (null not allowed)
     * @return HTTP 202 when request is accepted, 400 on BAD REQUEST, 500 on internal server error
     */
    @POST
    @Path("/{transactionId}/units/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkAtomicUpdateUnits(
        @PathParam("transactionId") String transactionId,
        JsonNode updateQueriesJson
    ) {
        try {
            SanityChecker.checkParameter(transactionId);

            TransactionModel transactionModel = getTransaction(transactionId);

            transactionService.ensureTransactionIsOpen(transactionModel);

            bulkAtomicUpdateMetadataService.checkThreshold(updateQueriesJson);

            ArrayNode queries = BulkAtomicUpdateModelUtils.getQueries(updateQueriesJson);

            List<BulkAtomicUpdateResult> bulkAtomicUpdateResults =
                bulkAtomicUpdateMetadataService.bulkAtomicUpdateUnits(transactionModel.getId(), queries, false);

            return new RequestResponseOK<BulkAtomicUpdateResult>()
                .addAllResults(bulkAtomicUpdateResults)
                .setHttpCode(ACCEPTED.getStatusCode())
                .toResponse();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("Bulk atomic update failed - Not Found. Transaction : '" + transactionId + "'", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | CollectInternalInvalidRequestException e) {
            LOGGER.error("Bulk atomic update failed - Bad request. Transaction : '" + transactionId + "'", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (CollectInternalException | RuntimeException e) {
            LOGGER.error(
                "Bulk atomic update failed - Internal server error. " + "Transaction : '" + transactionId + "'",
                e
            );
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    /**
     * Start a reclassification workflow on collect
     *
     * @param transactionId as transaction Id
     * @param reclassificationRequestJson as JsonNode
     * @return an archive unit result list with inherited rules
     */
    @POST
    @Path("/{transactionId}/reclassification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reclassification(
        @PathParam("transactionId") String transactionId,
        JsonNode reclassificationRequestJson
    ) {
        try {
            SanityChecker.checkParameter(transactionId);
            SanityChecker.checkJsonAll(reclassificationRequestJson);

            TransactionModel transaction = getTransaction(transactionId);

            transactionService.ensureTransactionIsOpen(transaction);

            // Start workflow
            String operationId = VitamThreadUtils.getVitamSession().getRequestId();
            WorkFlowExecutionContext executionContext = WorkFlowExecutionContext.COLLECT;

            WorkspaceClientFactory workspaceClientFactory = WorkspaceClientFactory.getInstance(
                WorkFlowExecutionContext.VITAM
            );
            try (
                ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance(
                    executionContext
                ).getClient();
                LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance(
                    executionContext
                ).getClient();
                WorkspaceClient workspaceClient = workspaceClientFactory.getClient()
            ) {
                GUID operationGUID = GUIDReader.getGUID(operationId);
                final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                    operationGUID,
                    Contexts.COLLECT_RECLASSIFICATION.getEventType(),
                    operationGUID,
                    LogbookTypeProcess.COLLECT_RECLASSIFICATION,
                    STARTED,
                    VitamLogbookMessages.getLabelOp("COLLECT_RECLASSIFICATION.STARTED") + " : " + operationGUID,
                    operationGUID
                );

                // Enhancement to do : add transaction to logbook parameters?

                logbookOperationsClient.create(initParameters);

                workspaceClient.createContainer(operationId);

                // Add request as input parameter
                String objectName = "request.json";
                workspaceClient.putObject(operationId, objectName, writeToInpustream(reclassificationRequestJson));

                objectName = "transaction.json";
                ObjectNode transactionParameterJson = JsonHandler.createObjectNode();
                transactionParameterJson.put("transactionId", transactionId);
                workspaceClient.putObject(operationId, objectName, writeToInpustream(transactionParameterJson));

                // store original query in workspace
                workspaceClient.putObject(
                    operationId,
                    OperationContextMonitor.OperationContextFileName,
                    writeToInpustream(OperationContextModel.get(reclassificationRequestJson))
                );

                // compress file to backup
                OperationContextMonitor.compressInWorkspace(
                    workspaceClientFactory,
                    operationId,
                    LogbookTypeProcess.COLLECT_RECLASSIFICATION,
                    OperationContextMonitor.OperationContextFileName
                );

                processingClient.initVitamProcess(operationId, Contexts.COLLECT_RECLASSIFICATION.name());

                RequestResponse<ItemStatus> jsonNodeRequestResponse = processingClient.executeOperationProcess(
                    operationId,
                    Contexts.COLLECT_RECLASSIFICATION.name(),
                    ProcessAction.RESUME.getValue()
                );
                return jsonNodeRequestResponse.toResponse();
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (BadRequestException e) {
            LOGGER.error("Error starting reclassification workflow - Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error starting reclassification workflow - Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    /**
     * Starts an elimination action workflow on Collect.
     */

    @POST
    @Path("/{transactionId}/elimination/action")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEliminationActionWorkflow(
        @PathParam("transactionId") String transactionId,
        EliminationRequestBody eliminationRequestBody
    ) {
        try {
            SanityChecker.checkParameter(transactionId);
            TransactionModel transaction = getTransaction(transactionId);

            transactionService.ensureTransactionIsOpen(transaction);

            return transactionService.startEliminationActionWorkflow(
                transactionId,
                eliminationRequestBody,
                Contexts.COLLECT_ELIMINATION_ACTION
            );
        } catch (
            VitamClientException
            | CollectInternalException
            | InternalServerException
            | OperationContextException
            | ContentAddressableStorageServerException
            | LogbookClientServerException e
        ) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (
            BadRequestException
            | InvalidGuidOperationException
            | LogbookClientAlreadyExistsException
            | LogbookClientBadRequestException
            | InvalidParseOperationException
            | InvalidCreateOperationException e
        ) {
            LOGGER.error("Error starting elimination workflow - Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error starting elimination workflow - Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    /**
     * Starts a deletion workflow on Collect.
     */

    @POST
    @Path("/{transactionId}/deletion/action")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startDeletionWorkflow(
        @PathParam("transactionId") String transactionId,
        DeletionRequestBody deletionRequestBody
    ) {
        try {
            SanityChecker.checkParameter(transactionId);
            TransactionModel transaction = getTransaction(transactionId);

            transactionService.ensureTransactionIsOpen(transaction);

            return transactionService.startDeletionWorkflow(
                transactionId,
                deletionRequestBody,
                Contexts.COLLECT_DELETION_ACTION
            );
        } catch (
            VitamClientException
            | InternalServerException
            | OperationContextException
            | ContentAddressableStorageServerException
            | LogbookClientServerException e
        ) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (
            InvalidCreateOperationException
            | BadRequestException
            | InvalidGuidOperationException
            | LogbookClientAlreadyExistsException
            | LogbookClientBadRequestException
            | InvalidParseOperationException e
        ) {
            LOGGER.error("Error starting deletion workflow - Bad Request", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error starting deletion workflow - Internal Server Error", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    /**
     * Upload compressed SIP as Stream on transaction, will be uncompressed in workspace-collect.</br>
     * </br>
     *
     * @param contentType the header Content-Type (zip, tar, ...)
     * @param uploadedInputStream the stream to upload
     * @return Response with operation ID
     */
    @POST
    @Path("/{transactionId}/uploadSip")
    @Consumes(
        {
            MediaType.APPLICATION_OCTET_STREAM,
            CommonMediaType.ZIP,
            CommonMediaType.XGZIP,
            CommonMediaType.GZIP,
            CommonMediaType.TAR,
            CommonMediaType.BZIP2,
        }
    )
    public Response uploadSipAsStreamToTransaction(
        @PathParam("transactionId") String transactionId,
        @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
        InputStream uploadedInputStream
    ) {
        try {
            SanityChecker.checkParameter(transactionId, contentType);
            var operationIdDto = new UploadSipResult(
                transactionService.uploadSipOnTransaction(transactionId, contentType, uploadedInputStream)
            );
            return new RequestResponseOK<UploadSipResult>()
                .addResult(operationIdDto)
                .setHttpCode(Response.Status.OK.getStatusCode())
                .toResponse();
        } catch (CollectInternalNotFoundException e) {
            LOGGER.error("Error when uploading SIP to transaction. Not found", e);
            return CollectRequestResponse.toVitamError(NOT_FOUND, e.getLocalizedMessage());
        } catch (
            BadRequestException
            | LogbookClientAlreadyExistsException
            | InvalidParseOperationException
            | IllegalArgumentException
            | InvalidGuidOperationException
            | LogbookClientBadRequestException
            | CollectInternalInvalidRequestException
            | ContentAddressableStorageNotFoundException e
        ) {
            LOGGER.error("Error when uploading SIP to transaction. Bad Request: {}", e.getMessage(), e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            LOGGER.error("Error when uploading SIP to transaction. Internal Server Error: {}", e.getMessage(), e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    private TransactionModel getTransaction(String transactionId) throws CollectInternalException {
        Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);
        if (transactionModel.isEmpty()) {
            throw new CollectInternalNotFoundException("No such transaction '" + transactionId + "'");
        }
        return transactionModel.get();
    }
}
