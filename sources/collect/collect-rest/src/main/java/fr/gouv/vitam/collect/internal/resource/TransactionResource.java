/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import fr.gouv.vitam.collect.internal.dto.IngestDto;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.helpers.builders.CollectModelBuilder;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Optional;

import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_BINARY_UPSERT;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_CLOSE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_OBJECT_UPSERT;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_SEND;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_CREATE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/collect/v1")
public class TransactionResource extends ApplicationStatusResource {
    public static final String ERROR_WHILE_TRYING_TO_SAVE_UNITS = "Error while trying to save units";
    public static final String SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUIID =
        "SIP ingest operation can't provide a null operationGuiid";
    public static final String SIP_GENERATED_MANIFEST_CAN_T_BE_NULL = "SIP generated manifest can't be null";
    private final CollectService collectService;
    private final TransactionService transactionService;
    private final SipService sipService;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionResource.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";
    private static final String OPI = "#opi";
    private static final String ID = "#id";

    public TransactionResource(CollectService collectService, TransactionService transactionService,
        SipService sipService) {
        this.collectService = collectService;
        this.transactionService = transactionService;
        this.sipService = sipService;
    }

    @Path("/transactions")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_CREATE, description = "Créer une transaction")
    public Response initTransaction(TransactionDto transactionDto) {
        try {
            ParametersChecker.checkParameter("You must supply transaction datas!", transactionDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(transactionDto));

            String requestId = collectService.createRequestId();
            transactionDto.setId(requestId);
            CollectModel collectModel = new CollectModelBuilder()
                .withId(transactionDto.getId())
                .withArchivalAgencyIdentifier(transactionDto.getArchivalAgencyIdentifier())
                .withTransferingAgencyIdentifier(transactionDto.getTransferingAgencyIdentifier())
                .withOriginatingAgencyIdentifier(transactionDto.getOriginatingAgencyIdentifier())
                .withArchivalProfile(transactionDto.getArchivalProfile())
                .withComment(transactionDto.getComment())
                .withStatus(TransactionStatus.OPEN)
                .build();
            collectService.createCollect(collectModel);
            return CollectRequestResponse.toResponseOK(transactionDto);
        } catch (CollectException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/transactions/{transactionId}/units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_CREATE, description = "Créer une unité archivistique")
    public Response uploadArchiveUnit(@PathParam("transactionId") String transactionId, JsonNode unitJsonNode) {

        try {
            SanityChecker.checkParameter(transactionId);
            SanityChecker.checkJsonAll(unitJsonNode);

            Optional<CollectModel> collectModel = collectService.findCollect(transactionId);

            if (collectModel.isEmpty() || !collectService.checkStatus(collectModel.get(), TransactionStatus.OPEN)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            ObjectNode unitObjectNode = JsonHandler.getFromJsonNode(unitJsonNode, ObjectNode.class);
            unitObjectNode.put(ID, collectService.createRequestId());
            unitObjectNode.put(OPI, transactionId);
            JsonNode savedUnitJsonNode = transactionService.saveArchiveUnitInMetaData(unitObjectNode);

            if (savedUnitJsonNode == null) {
                LOGGER.error(ERROR_WHILE_TRYING_TO_SAVE_UNITS);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, ERROR_WHILE_TRYING_TO_SAVE_UNITS);
            }
            return CollectRequestResponse.toResponseOK(savedUnitJsonNode);
        } catch (CollectException | InvalidParseOperationException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/units/{unitId}/objects/{usage}/{version}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_OBJECT_UPSERT, description = "ajouter ou modifier un objet group")
    public Response uploadObjectGroup(@PathParam("unitId") String unitId,
        @PathParam("usage") String usageString,
        @PathParam("version") Integer version,
        ObjectGroupDto objectGroupDto) {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            ParametersChecker.checkParameter("You must supply object datas!", objectGroupDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupDto));

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            transactionService.checkParameters(unitId, usage, version);
            ArchiveUnitModel archiveUnitModel = transactionService.getArchiveUnitModel(unitId);
            ObjectGroupDto savedObjectGroupDto =
                transactionService.saveObjectGroupInMetaData(archiveUnitModel, usage, version, objectGroupDto);

            return CollectRequestResponse.toResponseOK(savedObjectGroupDto);
        } catch (CollectException e) {
            LOGGER.error("Error while trying to save objects : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error while trying to save objects : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_BINARY_UPSERT, description = "ajouter ou modifier un binaire")
    public Response upload(@PathParam("unitId") String unitId,
        @PathParam("usage") String usageString,
        @PathParam("version") Integer version,
        InputStream uploadedInputStream) throws CollectException {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            SanityChecker.checkParameter(String.valueOf(version.intValue()));
            ParametersChecker.checkParameter("You must supply a file!", uploadedInputStream);

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            transactionService.checkParameters(unitId, usage, version);
            ArchiveUnitModel archiveUnitModel = transactionService.getArchiveUnitModel(unitId);
            DbObjectGroupModel dbObjectGroupModel = transactionService.getDbObjectGroup(archiveUnitModel);
            transactionService.addBinaryInfoToQualifier(dbObjectGroupModel, usage, version, uploadedInputStream);

            return Response.status(OK).build();
        } catch (CollectException e) {
            // TODO : Manage rollback -> delete file ?
            LOGGER.debug("An error occurs when try to fetch data from database : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to fetch data from database : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }

    }

    @Path("/transactions/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_CLOSE, description = "Fermer une transaction")
    public Response closeTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            collectService.closeTransaction(transactionId);
            return Response.status(OK).build();
        } catch (CollectException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/transactions/{transactionId}/send")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_SEND, description = "Envoyer une transaction")
    public Response generateAndSendSip(@PathParam("transactionId") String transactionId) {

        try {
            SanityChecker.checkParameter(transactionId);

            Optional<CollectModel> collectModel = collectService.findCollect(transactionId);
            if (collectModel.isEmpty() || !collectService.checkStatus(collectModel.get(), TransactionStatus.CLOSE)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            String digest = sipService.generateSip(collectModel.get());
            if (digest == null) {
                LOGGER.error(SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
            }

            final String operationGuiid = sipService.ingest(collectModel.get(), digest);
            if (operationGuiid == null) {
                LOGGER.error(SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUIID);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR,
                    SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUIID);
            }

            CollectModel currentCollectModel = collectModel.get();
            currentCollectModel.setStatus(TransactionStatus.SENT);
            collectService.replaceCollect(currentCollectModel);

            return CollectRequestResponse.toResponseOK(new IngestDto(operationGuiid));
        } catch (CollectException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }


}