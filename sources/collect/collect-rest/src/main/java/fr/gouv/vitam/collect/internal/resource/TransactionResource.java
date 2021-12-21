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
import fr.gouv.vitam.collect.internal.dto.ArchiveUnitDto;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.GenerateSipService;
import fr.gouv.vitam.collect.internal.service.IngestSipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.objectgroup.FormatIdentificationModel;
import fr.gouv.vitam.metadata.api.exception.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/transactions")
public class TransactionResource {
    private final CollectService collectService;
    private final TransactionService transactionService;
    private final GenerateSipService generateSipService;
    private final IngestSipService ingestSipService;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionResource.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id";

    public TransactionResource(CollectService collectService, TransactionService transactionService, GenerateSipService generateSipService, IngestSipService ingestSipService) {
        this.collectService = collectService;
        this.transactionService = transactionService;
        this.generateSipService = generateSipService;
        this.ingestSipService = ingestSipService;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RequestResponseOK<TransactionDto> initTransaction() throws InvalidParseOperationException {
        String requestId = collectService.createRequestId();
        TransactionDto transactionDto = new TransactionDto(requestId);
        collectService.createCollect(new CollectModel(requestId));
        return new RequestResponseOK<TransactionDto>()
                .addResult(transactionDto).setHttpCode(Response.Status.OK.getStatusCode());
    }


    @Path("/{transactionId}/archiveunits")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RequestResponseOK<ArchiveUnitDto> uploadArchiveUnit(@PathParam("transactionId") String transactionId,
                                                               ArchiveUnitDto archiveUnitDto) throws InvalidParseOperationException {

        Optional<CollectModel> collectModel = collectService.findCollect(transactionId);
        archiveUnitDto.setId(collectService.createRequestId());
        archiveUnitDto.setTransactionId(transactionId);
        if (collectModel.isEmpty()) {
            LOGGER.debug(TRANSACTION_NOT_FOUND);
            return new RequestResponseOK<ArchiveUnitDto>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
        JsonNode jsonNode = transactionService.saveArchiveUnitInMetaData(archiveUnitDto);
        if (jsonNode != null) {
            return new RequestResponseOK<ArchiveUnitDto>().addResult(archiveUnitDto)
                    .setHttpCode(Integer.parseInt(String.valueOf(jsonNode.get("httpCode"))));
        }
        return new RequestResponseOK<ArchiveUnitDto>().setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


    @Path("/{transactionId}/archiveunits/{archiveUnitId}/gots")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RequestResponseOK<ObjectGroupDto> uploadObjectGroup(@PathParam("transactionId") String transactionId, @PathParam("archiveUnitId") String archiveUnitId, ObjectGroupDto objectGroupDto) throws InvalidParseOperationException {

        if (transactionId == null || archiveUnitId == null) {
            LOGGER.debug("archiveUnitId({}) or transactionId({}) can't be null", archiveUnitId);
            return new RequestResponseOK<ObjectGroupDto>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        Optional<CollectModel> collectModel = collectService.findCollect(transactionId);
        if (collectModel.isEmpty()) {
            LOGGER.debug(TRANSACTION_NOT_FOUND);
            return new RequestResponseOK<ObjectGroupDto>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        if (null == transactionService.getArchiveUnitById(archiveUnitId).get(0)) {
            LOGGER.debug("Unable to find archiveUnit Id");
            return new RequestResponseOK<ObjectGroupDto>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        objectGroupDto.setId(collectService.createRequestId());
        JsonNode jsonNode = transactionService.saveObjectGroupInMetaData(objectGroupDto, archiveUnitId);
        if (jsonNode != null) {
            CollectModel transaction = collectModel.get();
            Set<String> listGots = transaction.getIdGots();
            listGots.add(objectGroupDto.getId());
            transaction.setIdGots(listGots);
            collectService.replaceCollect(transaction);
            return new RequestResponseOK<ObjectGroupDto>().addResult(objectGroupDto)
                    .setHttpCode(Integer.parseInt(String.valueOf(jsonNode.get("httpCode"))));
        }
        return new RequestResponseOK<ObjectGroupDto>().setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Path("/{transactionId}/archiveunits/{auId}/gots/{gotId}/binary/{usage}/")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@PathParam("transactionId") String transactionId,
                           @PathParam("auId") String archiveUnitId,
                           @PathParam("gotId") String gotId,
                           @HeaderParam("fileName") String fileName,
                           @DefaultValue("BinaryMaster") @PathParam("usage") String usage,
                           InputStream uploadedInputStream) throws InvalidParseOperationException {

        if (transactionId == null || archiveUnitId == null || gotId == null) {
            LOGGER.error("transactionId({}), archiveUnitId({}) or gotId({}) or fileName({}) can't be null", transactionId,
                    archiveUnitId, gotId, fileName);
            return Response.status(Response.Status.BAD_REQUEST).entity(null).build();
        }

        Optional<CollectModel> optionalCollectModel =
                transactionService.verifyAndGetCollectByTransaction(transactionId);

        if (optionalCollectModel.isEmpty()) {
            LOGGER.debug(TRANSACTION_NOT_FOUND);
            return Response.status(Response.Status.NOT_FOUND).entity(null).build();
        }

        try {
            int sizeInputStream = uploadedInputStream.available();
            transactionService.uploadVerifications(archiveUnitId, gotId);
            if(null == fileName){
                fileName = collectService.createRequestId();
            }
            String digest = transactionService.pushSipStreamToWorkspace(transactionId, uploadedInputStream, transactionService.sanitizeFileName(fileName));
            FormatIdentificationModel formatIdentifierResponse =  transactionService.getFormatIdentification(transactionId, fileName);
            transactionService.updateGotWithBinaryInfos(transactionId, gotId, usage,  transactionService.sanitizeFileName(fileName), digest, sizeInputStream, formatIdentifierResponse);

            return Response.status(Response.Status.OK).build();
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException | MetadataInvalidSelectException | CollectException | IOException e) {
            LOGGER.debug("An error occurs when try to fetch data from database : {}", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(null).build();
        }

    }

    @Path("/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RequestResponseOK<TransactionDto> generateSip(
            @PathParam("transactionId") String transactionId
    ) throws XMLStreamException, InvalidParseOperationException, JAXBException {
        Optional<CollectModel> collectModel = collectService.findCollect(transactionId);
        if (collectModel.isEmpty()) {
            LOGGER.debug(TRANSACTION_NOT_FOUND);
            return new RequestResponseOK<TransactionDto>().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        try {
            String digest = generateSipService.generateSip(collectModel.get());
            if(digest != null){
                final String operationGuiid = ingestSipService.ingest(collectModel.get(), digest);
                if(operationGuiid != null){
                    TransactionDto transactionDto = new TransactionDto(operationGuiid);
                    return new RequestResponseOK<TransactionDto>()
                            .addResult(transactionDto).setHttpCode(Response.Status.OK.getStatusCode());
                }
            }
        } catch (Exception e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            return new RequestResponseOK<TransactionDto>().setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
        return new RequestResponseOK<TransactionDto>().setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


}
