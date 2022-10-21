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
package fr.gouv.vitam.collect.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.external.dto.IngestDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.service.MetadataService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.dsl.schema.Dsl;
import fr.gouv.vitam.common.dsl.schema.DslSchema;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_CLOSE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_ID_DELETE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_ID_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_SEND;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_READ;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/collect-external/v1/transactions")
public class TransactionResource {
    public static final String ERROR_WHILE_TRYING_TO_SAVE_UNITS = "Error while trying to save units";
    public static final String SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUID =
        "SIP ingest operation can't provide a null operationGuid";
    public static final String SIP_GENERATED_MANIFEST_CAN_T_BE_NULL = "SIP generated manifest can't be null";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionResource.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";
    private static final String OPI = "#opi";
    private static final String ID = "#id";
    private static final String UNIT_TYPE = "#unitType";
    private static final String INGEST = "INGEST";

    private final TransactionService transactionService;
    private final MetadataService metadataService;
    private final SipService sipService;

    public TransactionResource(TransactionService transactionService, SipService sipService,
        MetadataService metadataService) {
        this.transactionService = transactionService;
        this.sipService = sipService;
        this.metadataService = metadataService;
    }

    @Path("/{transactionId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_ID_READ, description = "retourner la transaction par son id")
    public Response getTransactionById(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);

            if (transactionModel.isEmpty()) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            TransactionDto transactionDto =
                CollectHelper.convertTransactionModelToTransactionDto(transactionModel.get());


            return CollectRequestResponse.toResponseOK(transactionDto);
        } catch (CollectException e) {
            LOGGER.error("Error when get transaction by Id : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when get transaction by Id : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_ID_DELETE, description = "Supprime une transaction par son id")
    public Response deleteTransactionById(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);

            if (transactionModel.isEmpty()) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            transactionService.deleteTransaction(transactionModel.get().getId());
            return Response.status(Response.Status.OK).build();
        } catch (CollectException e) {
            LOGGER.error("Error when delete transaction by Id : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when delete transaction by Id : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_CREATE, description = "Crée une unité archivistique et la rattache à la transaction courante")
    public Response uploadArchiveUnit(@PathParam("transactionId") String transactionId, JsonNode unitJsonNode) {

        try {
            SanityChecker.checkParameter(transactionId);
            SanityChecker.checkJsonAll(unitJsonNode);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);

            if (transactionModel.isEmpty() ||
                !transactionService.checkStatus(transactionModel.get(), TransactionStatus.OPEN)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            ObjectNode unitObjectNode = JsonHandler.getFromJsonNode(unitJsonNode, ObjectNode.class);
            unitObjectNode.put(ID, GUIDFactory.newUnitGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId());
            unitObjectNode.put(OPI, transactionId);
            unitObjectNode.put(UNIT_TYPE, INGEST);
            JsonNode savedUnitJsonNode = metadataService.saveArchiveUnit(unitObjectNode);

            if (savedUnitJsonNode == null) {
                LOGGER.error(ERROR_WHILE_TRYING_TO_SAVE_UNITS);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, ERROR_WHILE_TRYING_TO_SAVE_UNITS);
            }
            return CollectRequestResponse.toResponseOK(unitObjectNode);
        } catch (CollectException | InvalidParseOperationException e) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_READ, description = "Récupére toutes les unités archivistique")
    public Response selectUnits(@PathParam("transactionId") String transactionId,
        @Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode jsonQuery) {
        try {
            final JsonNode results = metadataService.selectUnits(jsonQuery, transactionId);
            return Response.status(Response.Status.OK).entity(results).build();
        } catch (CollectException e) {
            LOGGER.error("Error when getting units in metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_CLOSE, description = "Ferme une transaction")
    public Response closeTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            transactionService.closeTransaction(transactionId);
            return Response.status(OK).build();
        } catch (CollectException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/send")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_SEND, description = "Envoi vers VITAM la transaction")
    public Response generateAndSendSip(@PathParam("transactionId") String transactionId) throws CollectException {
        TransactionModel transaction = null;
        try {
            SanityChecker.checkParameter(transactionId);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);
            if (transactionModel.isEmpty() ||
                !transactionService.checkStatus(transactionModel.get(), TransactionStatus.READY)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }
            transaction = transactionModel.get();
            transactionService.changeStatusTransaction(TransactionStatus.SENDING, transaction);
            String digest = sipService.generateSip(transaction);
            if (digest == null) {
                LOGGER.error(SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
                transactionService.changeStatusTransaction(TransactionStatus.KO, transaction);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
            }

            final String operationGuiid = sipService.ingest(transaction, digest);
            if (operationGuiid == null) {
                LOGGER.error(SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUID);
                transactionService.changeStatusTransaction(TransactionStatus.KO, transaction);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR,
                    SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUID);
            }
            transaction.setVitamOperationId(operationGuiid);
            transactionService.changeStatusTransaction(TransactionStatus.SENT, transaction);

            return CollectRequestResponse.toResponseOK(new IngestDto(operationGuiid));
        } catch (CollectException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            transactionService.changeStatusTransaction(TransactionStatus.KO, transaction);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            transactionService.changeStatusTransaction(TransactionStatus.KO, transaction);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }
}
