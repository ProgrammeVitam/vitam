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
import fr.gouv.vitam.collect.internal.helpers.CollectModelBuilder;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/collect/v1")
public class TransactionResource extends ApplicationStatusResource {
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
    public RequestResponseOK<TransactionDto> initTransaction(TransactionDto transactionDto) throws InvalidParseOperationException {
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
        return new RequestResponseOK<TransactionDto>()
            .addResult(transactionDto).setHttpCode(Response.Status.OK.getStatusCode());
    }

    @Path("/transactions/{transactionId}/units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RequestResponseOK<JsonNode> uploadArchiveUnit(
        @PathParam("transactionId") String transactionId,
        JsonNode unitJsonNode) throws InvalidParseOperationException, CollectException {

        Optional<CollectModel> collectModel = collectService.findCollect(transactionId);

        if (collectModel.isEmpty() || !collectService.checkStatus(collectModel.get(), TransactionStatus.OPEN)) {
            LOGGER.debug(TRANSACTION_NOT_FOUND);
            return new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        ((ObjectNode)unitJsonNode).put(ID, collectService.createRequestId());
        ((ObjectNode)unitJsonNode).put(OPI, transactionId);
        JsonNode jsonNode = transactionService.saveArchiveUnitInMetaData(unitJsonNode);

        if (jsonNode == null) {
            LOGGER.debug("Error while trying to save units : {}", jsonNode.toPrettyString());
            return new RequestResponseOK<JsonNode>().setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        return new RequestResponseOK<JsonNode>().addResult(unitJsonNode)
            .setHttpCode(Integer.parseInt(String.valueOf(jsonNode.get("httpCode"))));
    }

    @Path("/units/{unitId}/objects/{usage}/{version}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RequestResponseOK<ObjectGroupDto> uploadObjectGroup(
        @PathParam("unitId") String unitId,
        @PathParam("usage") String usage,
        @PathParam("version") Integer version,
        ObjectGroupDto objectGroupDto) throws CollectException {

        if (usage == null || unitId == null || version == null) {
            LOGGER.error("usage({}), unitId({}) or version({}) can't be null", usage, unitId, version);
            return new RequestResponseOK<ObjectGroupDto>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        ArchiveUnitModel archiveUnitModel = transactionService.getArchiveUnitById(unitId);
        if (null == archiveUnitModel) {
            LOGGER.debug("Unable to find archiveUnit Id");
            return new RequestResponseOK<ObjectGroupDto>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        objectGroupDto.setId(collectService.createRequestId());

        try {
            transactionService.saveObjectGroupInMetaData(archiveUnitModel, usage, version, objectGroupDto);
        } catch (CollectException e) {
            LOGGER.debug("Error while trying to save objects : {}", e);
            return new RequestResponseOK<ObjectGroupDto>().setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        return new RequestResponseOK<ObjectGroupDto>().addResult(objectGroupDto)
            .setHttpCode(Response.Status.OK.getStatusCode());
    }

    @Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public RequestResponseOK<Object> upload(@PathParam("unitId") String unitId,
        @PathParam("usage") String usage,
        @PathParam("version") Integer version,
        InputStream uploadedInputStream) throws CollectException {

        if (usage == null || unitId == null || version == null) {
            LOGGER.error("usage({}), unitId({}) or version({}) can't be null", usage,
                unitId, version);
            return new RequestResponseOK<>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        ArchiveUnitModel archiveUnitModel = transactionService.getArchiveUnitById(unitId);
        if (null == archiveUnitModel) {
            LOGGER.debug("Unable to find archiveUnit Id");
            return new RequestResponseOK<>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        try {
            DbObjectGroupModel dbObjectGroupModel = transactionService.getDbObjectGroup(archiveUnitModel);
            transactionService.addBinaryInfoToQualifier(dbObjectGroupModel, usage, version, uploadedInputStream);
            return new RequestResponseOK<>().setHttpCode(Response.Status.OK.getStatusCode());
        } catch (CollectException e) {
            // TODO : Manage rollback -> delete file ?
            LOGGER.debug("An error occurs when try to fetch data from database : {}", e);
            return new RequestResponseOK<>().setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

    }

    @Path("/transactions/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RequestResponseOK<TransactionDto> generateSip(@PathParam("transactionId") String transactionId)
        throws InvalidParseOperationException {

        Optional<CollectModel> collectModel = collectService.findCollect(transactionId);
        if (collectModel.isEmpty() && !collectService.checkStatus(collectModel.get(), TransactionStatus.OPEN)) {
            LOGGER.debug(TRANSACTION_NOT_FOUND);
            return new RequestResponseOK<TransactionDto>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        try {
            String digest = sipService.generateSip(collectModel.get());
            if (digest == null) {
                return new RequestResponseOK<TransactionDto>().setHttpCode(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            }

            final String operationGuiid = sipService.ingest(collectModel.get(), digest);
            if (operationGuiid != null) {
                CollectModel currentCollectModel = collectModel.get();
                currentCollectModel.setStatus(TransactionStatus.SENT);
                collectService.replaceCollect(currentCollectModel);
                return new RequestResponseOK<TransactionDto>()
                    .addResult(new TransactionDto(operationGuiid)).setHttpCode(Response.Status.OK.getStatusCode());
            }

            return new RequestResponseOK<TransactionDto>().setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        } catch (Exception e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            return new RequestResponseOK<TransactionDto>().setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }


}
