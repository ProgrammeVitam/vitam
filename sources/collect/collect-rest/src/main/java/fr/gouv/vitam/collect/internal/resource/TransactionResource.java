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
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.exception.CollectVitamError;
import fr.gouv.vitam.collect.internal.helpers.CollectModelBuilder;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

@Path("/collect/v1")
public class TransactionResource extends ApplicationStatusResource {
    public static final String ERROR_WHILE_TRYING_TO_SAVE_UNITS = "Error while trying to save units";
    public static final String UNABLE_TO_FIND_ARCHIVE_UNIT_ID = "Unable to find archiveUnit Id";
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

    public TransactionResource(CollectService collectService, TransactionService transactionService, SipService sipService) {
        this.collectService = collectService;
        this.transactionService = transactionService;
        this.sipService = sipService;
    }

    @Path("/transactions")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response initTransaction(TransactionDto transactionDto) {
        // TODO : Sanity check
        try {
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
            String resMock = "{\n" +
                    "    \"httpCode\": 200,\n" +
                    "    \"$hits\": {\n" +
                    "        \"total\": 1,\n" +
                    "        \"offset\": 0,\n" +
                    "        \"limit\": 0,\n" +
                    "        \"size\": 1\n" +
                    "    },\n" +
                    "    \"$results\": [\n" +
                    "        {\n" +
                    "            \"id\": \""+ transactionDto.getId() +"\",\n" +
                    "            \"ArchivalAgencyIdentifier\": \"ArchivalAgencyIdentifier4\",\n" +
                    "            \"TransferingAgencyIdentifier\": \"TransferingAgencyIdentifier5\",\n" +
                    "            \"OriginatingAgencyIdentifier\": \"FRAN_NP_009913\",\n" +
                    "            \"ArchiveProfile\": \"ArchiveProfile\",\n" +
                    "            \"Comment\": \"Comments\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"$facetResults\": [],\n" +
                    "    \"$context\": {}\n" +
                    "}";
            return Response.status(Response.Status.OK).entity(resMock).build();
        } catch (CollectException e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(CollectVitamError.of(INTERNAL_SERVER_ERROR).withMessage(e.getLocalizedMessage()).build())
                .build();
        }
    }

    @Path("/transactions/{transactionId}/units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadArchiveUnit( @PathParam("transactionId") String transactionId, JsonNode unitJsonNode) {
        // TODO : Sanity check
        try {
            Optional<CollectModel> collectModel = collectService.findCollect(transactionId);

            if (collectModel.isEmpty() || !collectService.checkStatus(collectModel.get(), TransactionStatus.OPEN)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return Response.status(BAD_REQUEST)
                    .entity(CollectVitamError.of(BAD_REQUEST).withMessage(TRANSACTION_NOT_FOUND).build())
                    .build();
            }

            ObjectNode objectNode = JsonHandler.getFromJsonNode(unitJsonNode, ObjectNode.class);
            String ids = collectService.createRequestId();
            objectNode.put(ID, ids);
            objectNode.put(OPI, transactionId);
            JsonNode jsonNode = transactionService.saveArchiveUnitInMetaData(unitJsonNode);

            if (jsonNode == null) {
                LOGGER.error(ERROR_WHILE_TRYING_TO_SAVE_UNITS);
                return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(CollectVitamError.of(INTERNAL_SERVER_ERROR).withMessage(ERROR_WHILE_TRYING_TO_SAVE_UNITS)
                        .build())
                    .build();
            }
            String resMock = "{\n" +
                    "    \"httpCode\": 201,\n" +
                    "    \"$hits\": {\n" +
                    "        \"total\": 1,\n" +
                    "        \"offset\": 0,\n" +
                    "        \"limit\": 0,\n" +
                    "        \"size\": 1\n" +
                    "    },\n" +
                    "    \"$results\": [\n" +
                    "        {\n" +
                    "            \"#unitups\": [],\n" +
                    "            \"#min\": 1,\n" +
                    "            \"#max\": 1,\n" +
                    "            \"#allunitups\": [],\n" +
                    "            \"DescriptionLevel\": \"Item\",\n" +
                    "            \"Title\": \"My title3\",\n" +
                    "            \"Description\": \"Allemant. - Au chemin des Dames : le chateau et la ferme de la Motte totalement detruits.\",\n" +
                    "            \"Descriptions\": {\n" +
                    "                \"fr\": \"La legendes traduites en anglais.\"\n" +
                    "            },\n" +
                    "            \"Status\": \"Pret\",\n" +
                    "            \"Tag\": [\n" +
                    "                \"Grande Collecte\"\n" +
                    "            ],\n" +
                    "            \"Source\": \"Famille Herve, CP1\",\n" +
                    "            \"CreatedDate\": \"2014-06-12T09:31:00\",\n" +
                    "            \"TransactedDate\": \"2014-06-12T09:31:00\",\n" +
                    "            \"#management\": {},\n" +
                    "            \"#originating_agencies\": [],\n" +
                    "            \"#id\": \""+ ids +"\",\n" +
                    "            \"#opi\": \"aeeaaaaaach2brfeabbykal7ds53vdqaaaaq\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"$facetResults\": [],\n" +
                    "    \"$context\": {}\n" +
                    "}";
            return Response.status(Response.Status.OK).entity(resMock).build();
        } catch (CollectException | InvalidParseOperationException e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(CollectVitamError.of(INTERNAL_SERVER_ERROR).withMessage(e.getLocalizedMessage()).build())
                .build();
        }
    }

    @Path("/units/{unitId}/objects/{usage}/{version}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadObjectGroup(@PathParam("unitId") String unitId,
                                      @PathParam("usage") DataObjectVersionType usage,
                                      @PathParam("version") Integer version,
                                      ObjectGroupDto objectGroupDto) {
        // TODO : Sanity check
        try {
            ArchiveUnitModel archiveUnitModel = checkParametersAndGetArchiveUnitModel(unitId, usage, version);
            transactionService.saveObjectGroupInMetaData(archiveUnitModel, usage, version, objectGroupDto);

            String resMock = "{\n" +
                    "    \"httpCode\": 200,\n" +
                    "    \"$hits\": {\n" +
                    "        \"total\": 1,\n" +
                    "        \"offset\": 0,\n" +
                    "        \"limit\": 0,\n" +
                    "        \"size\": 1\n" +
                    "    },\n" +
                    "    \"$results\": [\n" +
                    "        {\n" +
                    "            \"id\": \"aeeaaaaaach2brfeabbykal7egm5ysyaaaaq\",\n" +
                    "            \"fileInfo\": {\n" +
                    "                \"lastModified\": \"2022-02-22T13:23:25.846\",\n" +
                    "                \"filename\": \"plan.txt\"\n" +
                    "            }\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"$facetResults\": [],\n" +
                    "    \"$context\": {}\n" +
                    "}";

            return Response.status(OK).entity(resMock).build();
        } catch (CollectException e) {
            LOGGER.error("Error while trying to save objects : {}", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(CollectVitamError.of(INTERNAL_SERVER_ERROR).withMessage(e.getLocalizedMessage()).build())
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(BAD_REQUEST)
                .entity(CollectVitamError.of(BAD_REQUEST).withMessage(e.getLocalizedMessage()).build())
                .build();
        }
    }

    @Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@PathParam("unitId") String unitId,
                           @PathParam("usage") DataObjectVersionType usage,
                           @PathParam("version") Integer version,
                           InputStream uploadedInputStream) throws CollectException {
        // TODO : Sanity check
        try {
            ArchiveUnitModel archiveUnitModel = checkParametersAndGetArchiveUnitModel(unitId, usage, version);
            DbObjectGroupModel dbObjectGroupModel = transactionService.getDbObjectGroup(archiveUnitModel);
            transactionService.addBinaryInfoToQualifier(dbObjectGroupModel, usage, version, uploadedInputStream);
            return Response.status(OK).build();
        } catch (CollectException e) {
            // TODO : Manage rollback -> delete file ?
            LOGGER.debug("An error occurs when try to fetch data from database : {}", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(CollectVitamError.of(INTERNAL_SERVER_ERROR).withMessage(e.getLocalizedMessage()).build())
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(BAD_REQUEST)
                .entity(CollectVitamError.of(BAD_REQUEST).withMessage(e.getLocalizedMessage()).build())
                .build();
        }

    }

    private ArchiveUnitModel checkParametersAndGetArchiveUnitModel(String unitId, DataObjectVersionType usage,
        Integer version) throws CollectException {
        if (usage == null || unitId == null || version == null) {
            LOGGER.error("usage({}), unitId({}) or version({}) can't be null", usage, unitId, version);
            throw new IllegalArgumentException("usage, unitId or version can't be null");
        }

        ArchiveUnitModel archiveUnitModel = transactionService.getArchiveUnitById(unitId);
        if (archiveUnitModel == null) {
            LOGGER.error(UNABLE_TO_FIND_ARCHIVE_UNIT_ID);
            throw new CollectException(UNABLE_TO_FIND_ARCHIVE_UNIT_ID);
        }
        return archiveUnitModel;
    }

    @Path("/transactions/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response closeTransaction(@PathParam("transactionId") String transactionId) {
        try {
            // TODO : Sanity check
            Optional<CollectModel> collectModel = collectService.findCollect(transactionId);
            if (collectModel.isEmpty() || !collectService.checkStatus(collectModel.get(), TransactionStatus.OPEN)) {
                LOGGER.debug(TRANSACTION_NOT_FOUND);
                Response.status(OK)
                    .entity(CollectVitamError.of(BAD_REQUEST).withMessage(TRANSACTION_NOT_FOUND).build())
                    .build();
            }
            CollectModel currentCollectModel = collectModel.get();
            currentCollectModel.setStatus(TransactionStatus.CLOSE);
            collectService.replaceCollect(currentCollectModel);
            return Response.status(OK).build();
        } catch (CollectException e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(CollectVitamError.of(INTERNAL_SERVER_ERROR).withMessage(e.getLocalizedMessage()).build())
                .build();
        }
    }

    @Path("/transactions/{transactionId}/send")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateAndSendSip(@PathParam("transactionId") String transactionId) {
        // TODO : Sanity check
        try {
            Optional<CollectModel> collectModel = collectService.findCollect(transactionId);
            if (collectModel.isEmpty() || !collectService.checkStatus(collectModel.get(), TransactionStatus.CLOSE)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return Response.status(BAD_REQUEST)
                    .entity(CollectVitamError.of(BAD_REQUEST).withMessage(TRANSACTION_NOT_FOUND).build())
                    .build();
            }

            String digest = sipService.generateSip(collectModel.get());
            if (digest == null) {
                LOGGER.error(SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
                return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(CollectVitamError.of(INTERNAL_SERVER_ERROR)
                        .withMessage(SIP_GENERATED_MANIFEST_CAN_T_BE_NULL).build())
                    .build();
            }

            final String operationGuiid = sipService.ingest(collectModel.get(), digest);
            if (operationGuiid == null) {
                LOGGER.error(SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUIID);
                return Response.status(INTERNAL_SERVER_ERROR).entity(CollectVitamError.of(INTERNAL_SERVER_ERROR).
                        withMessage(SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUIID).build())
                    .build();
            }

            CollectModel currentCollectModel = collectModel.get();
            currentCollectModel.setStatus(TransactionStatus.SENT);
            collectService.replaceCollect(currentCollectModel);
            String resMock= "{\n" +
                    "    \"httpCode\": 200,\n" +
                    "    \"$hits\": {\n" +
                    "        \"total\": 1,\n" +
                    "        \"offset\": 0,\n" +
                    "        \"limit\": 0,\n" +
                    "        \"size\": 1\n" +
                    "    },\n" +
                    "    \"$results\": [\n" +
                    "        {\n" +
                    "            \"id\": \""+operationGuiid+"\",\n" +
                    "            \"ArchivalAgencyIdentifier\": null,\n" +
                    "            \"TransferingAgencyIdentifier\": null,\n" +
                    "            \"OriginatingAgencyIdentifier\": null,\n" +
                    "            \"ArchiveProfile\": null,\n" +
                    "            \"Comment\": null\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"$facetResults\": [],\n" +
                    "    \"$context\": {}\n" +
                    "}";
            return Response.status(OK).entity(resMock).build();
        } catch (CollectException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(CollectVitamError.of(INTERNAL_SERVER_ERROR).withMessage(e.getLocalizedMessage()).build())
                .build();
        }
    }


}
